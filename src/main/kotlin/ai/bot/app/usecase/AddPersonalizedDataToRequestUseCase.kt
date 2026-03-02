package ai.bot.app.usecase

import ai.bot.app.data.repository.PersonalizedDataRepository

class AddPersonalizedDataToRequestUseCase(
    private val repository: PersonalizedDataRepository
) {
    operator fun invoke(input: String): String {
        val personalizedData = repository.get()
        
        return if (personalizedData != null) {
            val personalizedSection = buildString {
                append("Стиль: ${personalizedData.style ?: "не задан"}\n")
                append("Ограничения: ${personalizedData.constraints ?: "не заданы"}\n")
                append("Контекст: ${personalizedData.context ?: "не задан"}\n\n")
            }
            personalizedSection + input
        } else {
            input
        }
    }
}