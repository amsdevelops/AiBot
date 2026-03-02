package ai.bot.app.usecase

class SlidingWindowStrategyUseCase(
    private val addResponsesToRequestUseCase: AddSavedResponsesToRequestUseCase
) {
    operator fun invoke(input: String): String {
        // Добавляем сохраненные ответы в запрос
        return addResponsesToRequestUseCase(input)
    }
}