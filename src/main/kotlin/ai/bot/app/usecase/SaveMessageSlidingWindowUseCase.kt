package ai.bot.app.usecase

import ai.bot.app.data.repository.ResponsesRepository
import ai.bot.app.remote.model.OpenAIResponse

class SaveMessageSlidingWindowUseCase(
    private val repository: ResponsesRepository
) {
    operator fun invoke(response: OpenAIResponse, message: String) {
        val responseText = response.output
            .firstOrNull { it.role == "assistant" }
            ?.content
            ?.firstOrNull()
            ?.text
        repository.add(responseText + message)
        
        // Затем проверяем и удаляем старые, если нужно
        val count = repository.count()
        if (count > 10) {
            val oldRecordsToDelete = count - 10
            repository.deleteOldRecords(oldRecordsToDelete)
        }
    }
}
