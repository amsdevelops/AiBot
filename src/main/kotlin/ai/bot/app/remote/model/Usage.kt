package ai.bot.app.remote.model

import com.google.gson.annotations.SerializedName

data class Usage(
    @SerializedName("input_tokens") val inputTokens: Int,
    @SerializedName("input_tokens_details") val inputTokensDetails: InputTokensDetails,
    @SerializedName("output_tokens") val outputTokens: Int,
    @SerializedName("output_tokens_details") val outputTokensDetails: OutputTokensDetails,
    @SerializedName("total_tokens") val totalTokens: Int
)