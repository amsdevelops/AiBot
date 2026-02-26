package ai.bot.app.usecase

import ai.bot.app.data.repository.ResponsesRepository
import ai.bot.app.remote.model.OpenAIResponse
import ai.bot.app.remote.usecase.GetOpenAIResponseUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SaveResponseTextUseCase(
    private val repository: ResponsesRepository
) {
    suspend operator fun invoke(response: OpenAIResponse, messageText: String? = null) {

        // Проверяем количество записей
        val count = repository.count()
        if (count > 10) {
            // Получаем 5 самых старых записей
            val oldRecords = repository.getOldRecords(8)
            // Удаляем эти записи
            repository.deleteOldRecords(8)

            // Формируем промт для резюмирования
            val summaryPrompt = "Summarize the following conversation:\n\n$oldRecords"

            // Получаем резюме от OpenAI
            val summaryResult = withContext(Dispatchers.IO) {
                GetOpenAIResponseUseCase(
                    input = summaryPrompt,
                    previousResponseId = null,
                    isStoreEnabled = false,
                    temperature = 0.3,
                    model = "gpt-4o"
                )
            }

            // Сохраняем резюме
            summaryResult.getOrNull()?.let { summary ->
                summary.output.firstOrNull { it.role == "assistant" }
                    ?.content
                    ?.firstOrNull()
                    ?.text
                    ?.let { repository.add(it) }
            }
        }
        // Сначала сохраняем текст сообщения пользователя, если он есть
        messageText?.let { repository.add(it) }

        // Получаем текст ответа от OpenAI
        val responseText = response.output
            .firstOrNull { it.role == "assistant" }
            ?.content
            ?.firstOrNull()
            ?.text

        responseText?.let { repository.add(it) }
    }
}