package ai.bot.app.remote.model

data class OpenAIRequest(
    val model: String,
    val input: String
)