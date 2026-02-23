package ai.bot.app.usecase

import ai.bot.app.data.repository.ResponsesRepository

class RemoveLastResponseUseCase(
    private val repository: ResponsesRepository
) {
    operator fun invoke() {
        val size = repository.size()
        if (size > 0) {
            repository.remove(size - 1)
        }
    }
}