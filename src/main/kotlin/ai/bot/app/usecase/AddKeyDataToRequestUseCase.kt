package ai.bot.app.usecase

import ai.bot.app.data.repository.KeyDataRepository

class AddKeyDataToRequestUseCase(
    private val keyDataRepository: KeyDataRepository
) {
    operator fun invoke(input: String): String {
        val keyData = keyDataRepository.getAll()
        return if (keyData.isNotEmpty()) {
            "$keyData\n\n$input"
        } else {
            input
        }
    }
}