package ai.bot.app.usecase

import ai.bot.app.data.repository.ResponsesRepository

class AddSavedResponsesToRequestUseCase(
    private val repository: ResponsesRepository
) {
    operator fun invoke(input: String): String {
        val savedResponses = repository.getAll()
        return if (savedResponses.isNotEmpty()) {
            val combinedResponses = savedResponses.joinToString("\n\n") { "Response: $it" }
            "$combinedResponses\n\n$input"
        } else {
            input
        }
    }
}
