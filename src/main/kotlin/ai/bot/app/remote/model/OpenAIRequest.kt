package ai.bot.app.remote.model

import com.google.gson.annotations.SerializedName

data class OpenAIRequest(
    @SerializedName("model") val model: String,
    @SerializedName("input") val input: String,
    @SerializedName("previous_response_id") val previousResponseId: String? = null,
    @SerializedName("store") val store: Boolean = false
)
