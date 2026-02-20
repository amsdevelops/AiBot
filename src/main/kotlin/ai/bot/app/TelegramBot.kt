package ai.bot.app

import ai.bot.app.remote.model.OpenAIResponse
import ai.bot.app.remote.usecase.GetOpenAIResponseUseCase
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

class TelegramBot(botToken: String) : TelegramLongPollingBot(botToken) {

    private var previousResponseId: String? = null
    private var isStoreEnabled: Boolean = false
    private var temperature: Double = 1.0

    @OptIn(DelicateCoroutinesApi::class)
    override fun onUpdateReceived(update: Update?) {
        if (update == null) return
        if (update.hasMessage() && update.message.hasText()) {
            val message = update.message
            val chatId = message.chatId
            val text = message.text

            when (text) {
                "/enablestore" -> isStoreEnabled = true
                "/disablestore" -> isStoreEnabled = false
                "/temperature" -> sendChatMessage(chatId)
                "0.7", "1.0", "1.2" -> handleTemperatureButton(chatId,text)
                else -> {
                    GlobalScope.launch {
                        val response = GetOpenAIResponseUseCase(
                            input = text,
                            previousResponseId = if (isStoreEnabled) previousResponseId else null,
                            isStoreEnabled = isStoreEnabled,
                            temperature = temperature,
                        )
                        if (isStoreEnabled) previousResponseId = response.getOrNull()?.id
                        when {
                            response.isSuccess -> sendTextMessage(chatId, getContent(response))
                            response.isFailure -> sendTextMessage(chatId, response.exceptionOrNull()?.message ?: "")
                        }
                    }
                }
            }
        }
    }

    private fun getContent(response: Result<OpenAIResponse>): String =
        response.getOrNull()?.output?.first { it.role == "assistant" }?.content?.first()?.text.toString()

    private fun sendTextMessage(chatId: Long, text: String) {
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

    private fun handleTemperatureButton(chatId: Long, text: String) {
        when (text) {
            "0.7" -> {
                temperature = 0.7
                sendTextMessage(chatId, "Temperature set to 0.7")
            }
            "1.0" -> {
                temperature = 1.0
                sendTextMessage(chatId, "Temperature set to 1.0")
            }
            "1.2" -> {
                temperature = 1.2
                sendTextMessage(chatId, "Temperature set to 1.2")
            }
        }
    }
    override fun getBotUsername(): String = "aibot"
}