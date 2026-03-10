package ai.bot.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val style: String,
    val format: String,
    val constraints: String
)