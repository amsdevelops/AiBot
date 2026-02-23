package ai.bot.app.usecase

import ai.bot.app.data.repository.ResponsesRepository

class ClearResponsesRepositoryUseCase(
    private val repository: ResponsesRepository
) {
    operator fun invoke() {
        repository.clear()
    }
}