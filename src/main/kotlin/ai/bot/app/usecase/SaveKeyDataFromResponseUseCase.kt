package ai.bot.app.usecase

import ai.bot.app.data.repository.ResponsesRepository
import ai.bot.app.data.repository.KeyDataRepository
import ai.bot.app.remote.model.OpenAIResponse
import ai.bot.app.remote.usecase.GetOpenAIResponseUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SaveKeyDataFromResponseUseCase(
    private val repository: ResponsesRepository,
    private val keyDataRepository: KeyDataRepository
) {
    suspend operator fun invoke(response: OpenAIResponse, messageText: String? = null) {
        // Сохраняем оригинальное сообщение пользователя
        messageText?.let { repository.add(it) }

        // Получаем текст ответа от OpenAI
        val responseText = response.output
            .firstOrNull { it.role == "assistant" }
            ?.content
            ?.firstOrNull()
            ?.text

        responseText?.let { 
            // Сохраняем оригинальный ответ
            repository.add(it)
            
            // Извлекаем и сохраняем ключевые данные
            extractAndSaveKeyData(it)
        }
    }

    private suspend fun extractAndSaveKeyData(responseText: String) {
        // Создаем промт для извлечения ключевых данных
        val keyDataPrompt = """
            Extract key information from the following text and return it in the format:
            - Key point 1
            - Key point 2
            - Key point 3
            ...
            
            Text: $responseText
        """.trimIndent()

        // Получаем ключевые данные от OpenAI
        val keyDataResult = withContext(Dispatchers.IO) {
            GetOpenAIResponseUseCase(
                input = keyDataPrompt,
                previousResponseId = null,
                isStoreEnabled = false,
                temperature = 0.3,
                model = "gpt-4o"
            )
        }

        // Сохраняем ключевые данные в отдельную таблицу
        keyDataResult.getOrNull()?.let { keyDataResponse ->
            val keyDataText = keyDataResponse.output
                .firstOrNull { it.role == "assistant" }
                ?.content
                ?.firstOrNull()
                ?.text

            keyDataText?.let { keyDataRepository.add(it) }
        }
    }
}