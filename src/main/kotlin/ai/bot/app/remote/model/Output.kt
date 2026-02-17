package ai.bot.app.remote.model

import com.google.gson.annotations.SerializedName

data class Output(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("status") val status: String,
    @SerializedName("content") val content: List<Content>,
    @SerializedName("role") val role: String
)