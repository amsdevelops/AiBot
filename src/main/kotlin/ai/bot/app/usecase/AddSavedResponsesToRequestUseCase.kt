package ai.bot.app.usecase

import ai.bot.app.data.repository.ResponsesRepository

class AddSavedResponsesToRequestUseCase(
    private val repository: ResponsesRepository
) {
    operator fun invoke(input: String): String {
        val savedResponses = repository.getAll()
        return if (savedResponses.isNotEmpty()) {
            "$savedResponses\n\n$input"
        } else {
            input
        }
    }
}
