package ai.bot.app.usecase

import ai.bot.app.data.repository.PersonalizedDataRepository

class AddPersonalizedDataUseCase(
    private val repository: PersonalizedDataRepository
) {
    operator fun invoke(style: List<String>, constraints: List<String>, context: List<String>) {
        repository.add(style, constraints, context)
    }
}