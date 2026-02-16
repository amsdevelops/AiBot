package ai.bot.app.remote.api

import ai.bot.app.remote.model.OpenAIRequest
import ai.bot.app.remote.model.OpenAIResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenAIApi {
    @POST("openai/v1/responses")
    suspend fun getResponse(
        @Header("Authorization") auth: String,
        @Body request: OpenAIRequest
    ): Response<OpenAIResponse>
}