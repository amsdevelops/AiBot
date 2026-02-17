package ai.bot.app.remote.usecase

import ai.bot.app.remote.api.RetrofitClient
import ai.bot.app.remote.model.OpenAIRequest
import ai.bot.app.remote.model.OpenAIResponse
import ai.bot.app.usecase.GetLocalPropertiesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GetOpenAIUseResponseCase {

    suspend operator fun invoke(input: String): Result<OpenAIResponse> {
        return withContext(Dispatchers.IO) {
            val apiKey = GetLocalPropertiesUseCase("AI_KEY")
                ?: return@withContext Result.failure(Exception("API key not found"))
            val request = OpenAIRequest("gpt-5.2", input)

            try {
                val response = RetrofitClient.api.getResponse("Bearer $apiKey", request)
                if (response.isSuccessful) {
                    response.body()?.let { Result.success(it) } ?: Result.failure(Exception("Empty response"))
                } else {
                    Result.failure(Exception("HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}