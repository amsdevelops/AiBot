package ai.bot.app.usecase

import ai.bot.app.remote.model.Usage

object CalculateCostUseCase {
    private val modelPrices = mapOf(
        "gpt-3.5-turbo" to mapOf(
            "input" to 0.000129,
            "output" to 0.000387
        ),
        "gpt-4o" to mapOf(
            "input" to 0.000645,
            "output" to 0.002577
        ),
        "gpt-5.2" to mapOf(
            "input" to 0.000531,
            "output" to 0.004245
        )
    )

    operator fun invoke(usage: Usage, model: String): Pair<Double, Double> {
        val prices = modelPrices[model] ?: mapOf("input" to 0.0, "output" to 0.0)

        val inputCost = usage.inputTokens * prices["input"]!!
        val outputCost = usage.outputTokens * prices["output"]!!
        return Pair(inputCost, outputCost)
    }
}