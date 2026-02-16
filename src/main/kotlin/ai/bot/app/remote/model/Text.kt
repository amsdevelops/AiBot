package ai.bot.app.remote.model

import com.google.gson.annotations.SerializedName

data class Text(
    @SerializedName("format") val format: Format,
    @SerializedName("verbosity") val verbosity: String
)