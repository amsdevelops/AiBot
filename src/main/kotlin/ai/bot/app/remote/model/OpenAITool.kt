package ai.bot.app.remote.model

import com.google.gson.annotations.SerializedName

class OpenAITool(
    @SerializedName("type") val type: String,
)