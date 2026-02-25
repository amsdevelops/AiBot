package ai.bot.app

import ai.bot.app.remote.model.OpenAIResponse
import ai.bot.app.remote.usecase.GetOpenAIResponseUseCase
import ai.bot.app.usecase.AddSavedResponsesToRequestUseCase
import ai.bot.app.usecase.CalculateCostUseCase
import ai.bot.app.usecase.CalculateResponseTimeUseCase
import ai.bot.app.usecase.ClearResponsesRepositoryUseCase
import ai.bot.app.usecase.SaveResponseTextUseCase
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

class TelegramBot(
    private val saveResponseTextUseCase: SaveResponseTextUseCase,
    private val addResponsesToRequestUseCase: AddSavedResponsesToRequestUseCase,
    private val clearResponsesRepositoryUseCase: ClearResponsesRepositoryUseCase,
    botToken: String,
) : TelegramLongPollingBot(botToken) {

    private var previousResponseId: String? = null
    private var isStoreEnabled: Boolean = false
    private var temperature: Double = 1.0
    private var selectedModel: String = "gpt-4o"

    @OptIn(DelicateCoroutinesApi::class)
    override fun onUpdateReceived(update: Update?) {
        if (update == null) return
        if (update.hasMessage() && update.message.hasText()) {
            val message = update.message
            val chatId = message.chatId
            val text = message.text

            when (text) {
                "/clear" -> sendClearRepositoryMessage(chatId)
                "/store" -> sendStoreControlMessage(chatId)
                "/temperature" -> sendChatMessage(chatId)
                "/model" -> sendModelSelectionMessage(chatId)
                "Включить хранение" -> {
                    isStoreEnabled = true
                    sendPlainTextMessage(chatId, "Хранение включено")
                }
                "Выключить хранение" -> {
                    isStoreEnabled = false
                    sendPlainTextMessage(chatId, "Хранение выключено")
                }
                "0.7", "1.0", "1.2" -> handleTemperatureButton(chatId, text)
                "gpt-3.5-turbo", "gpt-4o", "gpt-5.2" -> handleModelSelection(chatId, text)
                "Да, очистить" -> {
                    GlobalScope.launch {
                        clearResponsesRepositoryUseCase()
                    }
                    sendPlainTextMessage(chatId, "Чат очищен")
                }
                "Отмена" -> {
                    sendPlainTextMessage(chatId, "Очистка отменена")
                }
                else -> {
                    GlobalScope.launch {
                        val response = GetOpenAIResponseUseCase(
                            input = addResponsesToRequestUseCase(text),
                            previousResponseId = if (isStoreEnabled) previousResponseId else null,
                            isStoreEnabled = isStoreEnabled,
                            temperature = temperature,
                            model = selectedModel // Используем выбранную модель
                        )
                        response.getOrNull()?.let { saveResponseTextUseCase(it, text) }
                        if (isStoreEnabled) previousResponseId = response.getOrNull()?.id
                        when {
                            response.isSuccess -> sendTextMessage(chatId, getContent(response))
                            response.isFailure -> sendPlainTextMessage(chatId, response.exceptionOrNull()?.message ?: "")
                        }
                    }
                }
            }
        } else if (update.hasCallbackQuery()) {
            val callbackQuery = update.callbackQuery
            val callbackData = callbackQuery.data
            val chatId = callbackQuery.message.chatId

            when (callbackData) {
                "like" -> {
                    sendPlainTextMessage(chatId, "Спасибо за лайк!")
                }
                "dislike" -> {
                    sendPlainTextMessage(chatId, "Спасибо за обратную связь!")
                }
            }
        }
    }

    private fun getContent(response: Result<OpenAIResponse>): String {
        val responseValue = response.getOrNull() ?: return ""
        val usageInfo = "Usage: ${responseValue.usage.inputTokens} input tokens, ${responseValue.usage.outputTokens} output tokens"
        val (inputCost, outputCost) = CalculateCostUseCase(responseValue.usage, selectedModel)
        val inputCostInfo = "Input cost: ${String.format("%.4f", inputCost)} RUB"
        val outputCostInfo = "Output cost: ${String.format("%.4f", outputCost)} RUB"
        val totalCostInfo = "Total cost: ${String.format("%.4f", inputCost + outputCost)} RUB"
        val responseTime = CalculateResponseTimeUseCase(responseValue)
        val responseTimeInfo = "Response time: $responseTime"
        val modelInfo = "Model: $selectedModel"
        val content = responseValue.output.firstOrNull { it.role == "assistant" }?.content?.firstOrNull()?.text ?: ""

        return "$modelInfo\n\n$usageInfo\n\n$inputCostInfo\n$outputCostInfo\n\n$totalCostInfo\n\n$responseTimeInfo\n\n$content"
    }

    private fun sendTextMessage(chatId: Long, text: String) {
        val messages = splitText(text, 4096)
        messages.forEachIndexed { index, messageText ->
            val message = SendMessage().apply {
                this.chatId = chatId.toString()
                this.text = messageText
                // Добавим кнопки только к последнему сообщению
                if (index == messages.size - 1) {
                    this.replyMarkup = InlineKeyboardMarkup().apply {
                        keyboard = listOf(
                            listOf(
                                InlineKeyboardButton("👍").also { it.callbackData = "like" },
                                InlineKeyboardButton("👎").also { it.callbackData = "dislike" }
                            )
                        )
                    }
                }
            }

            try {
                execute(message)
            } catch (e: TelegramApiException) {
                e.printStackTrace()
            }
        }
    }

    private fun splitText(text: String, chunkSize: Int): List<String> {
        if (text.length <= chunkSize) {
            return listOf(text)
        }
        val chunks = mutableListOf<String>()
        var startIndex = 0
        while (startIndex < text.length) {
            val endIndex = minOf(startIndex + chunkSize, text.length)
            chunks.add(text.substring(startIndex, endIndex))
            startIndex = endIndex
        }
        return chunks
    }

    private fun sendPlainTextMessage(chatId: Long, text: String) {
        val message = SendMessage().apply {
            this.chatId = chatId.toString()
            this.text = text
        }

        try {
            execute(message)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }

    private fun sendChatMessage(chatId: Long) {
        val message = SendMessage().apply {
            this.chatId = chatId.toString()
            this.text = "Выберите действие:"
            this.replyMarkup = ReplyKeyboardMarkup().apply {
                keyboard = listOf(
                    KeyboardRow().apply {
                        add(KeyboardButton("0.7"))
                        add(KeyboardButton("1.0"))
                        add(KeyboardButton("1.2"))
                    }
                )
            }
        }

        try {
            execute(message)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }

    private fun sendModelSelectionMessage(chatId: Long) {
        val message = SendMessage().apply {
            this.chatId = chatId.toString()
            this.text = "Выберите модель:"
            this.replyMarkup = ReplyKeyboardMarkup().apply {
                keyboard = listOf(
                    KeyboardRow().apply {
                        add(KeyboardButton("gpt-3.5-turbo"))
                        add(KeyboardButton("gpt-4o"))
                        add(KeyboardButton("gpt-5.2"))
                    }
                )
            }
        }

        try {
            execute(message)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }

    private fun sendStoreControlMessage(chatId: Long) {
        val message = SendMessage().apply {
            this.chatId = chatId.toString()
            this.text = "Управление хранением:"
            this.replyMarkup = ReplyKeyboardMarkup().apply {
                keyboard = listOf(
                    KeyboardRow().apply {
                        add(KeyboardButton("Включить хранение"))
                        add(KeyboardButton("Выключить хранение"))
                    }
                )
            }
        }

        try {
            execute(message)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }

    private fun sendClearRepositoryMessage(chatId: Long) {
        val message = SendMessage().apply {
            this.chatId = chatId.toString()
            this.text = "Очистить чат?"
            this.replyMarkup = ReplyKeyboardMarkup().apply {
                keyboard = listOf(
                    KeyboardRow().apply {
                        add(KeyboardButton("Да, очистить"))
                        add(KeyboardButton("Отмена"))
                    }
                )
            }
        }

        try {
            execute(message)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }

    private fun handleTemperatureButton(chatId: Long, text: String) {
        when (text) {
            "0.7" -> {
                temperature = 0.7
                sendPlainTextMessage(chatId, "Temperature set to 0.7")
            }
            "1.0" -> {
                temperature = 1.0
                sendPlainTextMessage(chatId, "Temperature set to 1.0")
            }
            "1.2" -> {
                temperature = 1.2
                sendPlainTextMessage(chatId, "Temperature set to 1.2")
            }
        }
    }

    private fun handleModelSelection(chatId: Long, text: String) {
        selectedModel = text
        sendPlainTextMessage(chatId, "Model selected: $selectedModel")
    }

    override fun getBotUsername(): String = "aibot"
}