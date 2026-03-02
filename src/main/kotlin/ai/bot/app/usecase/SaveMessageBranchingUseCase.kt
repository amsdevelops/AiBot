package ai.bot.app.usecase

import ai.bot.app.data.repository.ResponsesRepository
import ai.bot.app.remote.model.OpenAIResponse

class SaveMessageBranchingUseCase(
    private val repository: ResponsesRepository
) {
    operator fun invoke(response: OpenAIResponse, branch: String, message: String) {
        val responseText = response.output
            .firstOrNull { it.role == "assistant" }
            ?.content
            ?.firstOrNull()
            ?.text + " " + message

        // Сохраняем ответ от OpenAI с указанием ветки
        repository.addWithBranch(responseText, branch)
    }
}