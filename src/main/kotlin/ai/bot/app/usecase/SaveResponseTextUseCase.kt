package ai.bot.app.usecase

import ai.bot.app.data.repository.ResponsesRepository
import ai.bot.app.remote.model.OpenAIResponse

class SaveResponseTextUseCase(
    private val repository: ResponsesRepository
) {
    operator fun invoke(response: OpenAIResponse, messageText: String? = null) {
        // Сначала сохраняем текст сообщения пользователя
        messageText?.let { repository.add(it) }

        // Затем сохраняем ответ от OpenAI
        val text = response.output
            .firstOrNull { it.role == "assistant" }
            ?.content
            ?.firstOrNull()
            ?.text

        text?.let { repository.add(it) }
    }
}