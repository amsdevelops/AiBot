package ai.bot.app.usecase

import ai.bot.app.data.repository.KeyDataRepository
import ai.bot.app.data.repository.ResponsesRepository

class ClearResponsesRepositoryUseCase(
    private val repository: ResponsesRepository,
    private val keyDataRepository: KeyDataRepository,
) {
    operator fun invoke() {
        repository.clear()
        keyDataRepository.clear()
    }
}