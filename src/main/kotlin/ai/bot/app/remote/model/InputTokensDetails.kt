package ai.bot.app.remote.model

import com.google.gson.annotations.SerializedName

data class InputTokensDetails(
    @SerializedName("cached_tokens") val cachedTokens: Int
)