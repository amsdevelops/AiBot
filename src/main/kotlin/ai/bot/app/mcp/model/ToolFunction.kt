import kotlinx.serialization.Serializable

@Serializable
data class ToolFunction(
    val type: String,
    val name: String,
    val description: String,
    val parameters: Parameters,
    val strict: Boolean = true
)

@Serializable
data class Parameters(
    val type: String,
    val properties: Map<String, Property>,
    val required: List<String>,
    val additionalProperties: Boolean = false
)

@Serializable
data class Property(
    val type: String,
    val description: String
)