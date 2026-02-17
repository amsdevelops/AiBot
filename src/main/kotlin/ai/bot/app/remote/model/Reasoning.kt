package ai.bot.app.remote.model

import com.google.gson.annotations.SerializedName

data class Reasoning(
    @SerializedName("effort") val effort: Any?,
    @SerializedName("summary") val summary: Any?
)