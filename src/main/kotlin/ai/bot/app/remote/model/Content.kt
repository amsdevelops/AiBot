package ai.bot.app.remote.model

import com.google.gson.annotations.SerializedName

data class Content(
    @SerializedName("type") val type: String,
    @SerializedName("annotations") val annotations: List<Any?>,
    @SerializedName("logprobs") val logprobs: List<Any?>,
    @SerializedName("text") val text: String
)