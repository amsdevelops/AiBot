package ai.bot.app

import ai.bot.app.remote.model.OpenAIResponse
import ai.bot.app.remote.usecase.GetOpenAIUseResponseCase
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

class TelegramBot(botToken: String) : TelegramLongPollingBot(botToken) {

    private var previousResponseId: String? = null
    private var isStoreEnabled: Boolean = false

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
                else -> {
                    GlobalScope.launch {
                        val response = GetOpenAIUseResponseCase(
                            input = text,
                            previousResponseId = if (isStoreEnabled) previousResponseId else null,
                            isStoreEnabled = isStoreEnabled,
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

    override fun getBotUsername(): String = "aibot"
}