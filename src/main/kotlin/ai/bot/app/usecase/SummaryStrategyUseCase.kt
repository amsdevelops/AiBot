package ai.bot.app.usecase

import ai.bot.app.data.repository.ResponsesRepository

class SummaryStrategyUseCase(
    private val repository: ResponsesRepository,
    private val addResponsesToRequestUseCase: AddSavedResponsesToRequestUseCase
) {
    operator fun invoke(input: String): String {
        // Получаем все сохраненные ответы
        val allResponses = repository.getAll()
        
        // Если есть сохраненные ответы, создаем резюме
        return if (allResponses.isNotEmpty()) {
            val summaryPrompt = "Summarize the following conversation:\n\n$allResponses"
            addResponsesToRequestUseCase(summaryPrompt)
        } else {
            input
        }
    }
}