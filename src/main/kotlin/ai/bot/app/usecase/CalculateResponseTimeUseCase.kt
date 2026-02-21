package ai.bot.app.usecase

import ai.bot.app.remote.model.OpenAIResponse

object CalculateResponseTimeUseCase {
    operator fun invoke(response: OpenAIResponse): String {
        val milliseconds = response.completedAt - response.createdAt
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val thousandths = (milliseconds % 1000)
        return "%02d:%02d.%03d".format(minutes, seconds, thousandths)
    }
}