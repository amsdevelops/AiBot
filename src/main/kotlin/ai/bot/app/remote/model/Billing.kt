package ai.bot.app.remote.model

import com.google.gson.annotations.SerializedName

data class Billing(
    @SerializedName("payer") val payer: String
)