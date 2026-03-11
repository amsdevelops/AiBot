package ai.bot.app.remote.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed interface Output {
    @SerialName("type")
    val type: String
}

@Serializable
data class TextContent(
    @SerialName("id") val id: String,
    @SerialName("contentType") override val type: String = "text_content",
    @SerialName("status") val status: String,
    @SerialName("content") val content: List<@Contextual Content>,
    @SerialName("role") val role: String
) : Output

@Serializable
data class ToolCall(
    @SerialName("id") val id: String,
    @SerialName("type") override val type: String = "tool_call",
    @SerialName("status") val status: String,
    @SerialName("arguments") val arguments: String,
    @SerialName("call_id") val call_id: String,
    @SerialName("name") val name: String
) : Output

@Serializable
data class Message(
    @SerialName("id") val id: String,
    @SerialName("type") override val type: String,
    @SerialName("status") val status: String,
    @SerialName("content") val content: List<MessageContent>,
    @SerialName("role") val role: String
): Output

@Serializable
data class MessageContent(
    @SerialName("type") val type: String,
    @SerialName("annotations") val annotations: List<@Contextual Any?>,
    @SerialName("logprobs") val logprobs: List<@Contextual Any?>,
    @SerialName("text") val text: String
)

fun String.parseArguments(): Map<String, String> {
    return try {
        Json.decodeFromString<Map<String, String>>(this)
    } catch (e: Exception) {
        emptyMap()
    }
}
