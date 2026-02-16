package ai.bot.app.remote.model

import com.google.gson.annotations.SerializedName

data class OutputTokensDetails(
    @SerializedName("reasoning_tokens") val reasoningTokens: Int
)