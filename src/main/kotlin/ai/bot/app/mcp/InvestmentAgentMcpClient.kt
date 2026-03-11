package ai.bot.app.mcp


import Parameters
import Property
import ToolFunction
import kotlinx.coroutines.future.await
import kotlinx.serialization.json.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.net.http.WebSocket
import java.net.http.WebSocket.Listener
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture

class InvestmentAgentMcpClient {
    
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()
    
    private val MCP_SERVER_URL = "http://localhost:8000"


    fun getTools(): List<ToolFunction>? {
        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$MCP_SERVER_URL/tools"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                Json.parseToJsonElement(response.body()).jsonArray.map { element ->
                    val obj = element.jsonObject
                    // Проверяем структуру JSON
                    val name = obj["name"]?.jsonPrimitive?.content
                        ?: obj["function"]?.jsonObject?.get("name")?.jsonPrimitive?.content.orEmpty()

                    val description = obj["description"]?.jsonPrimitive?.content
                        ?: obj["function"]?.jsonObject?.get("description")?.jsonPrimitive?.content

                    val type = obj["type"]?.jsonPrimitive?.content ?: ""
                    ToolFunction(
                        type = type,
                        name = name,
                        description = description ?: "",
                        parameters = parseParameters(obj["function"]?.jsonObject?.get("parameters")?.jsonObject ?: JsonObject(emptyMap()))
                    )
                }
            } else {
                null
            }
        } catch (e: Exception) {
            println("Ошибка при получении инструментов с MCP сервера: ${e.message}")
            null
        }
    }

    private fun parseParameters(paramsObj: JsonObject): Parameters {
        val propertiesObj = paramsObj["properties"]?.jsonObject ?: JsonObject(emptyMap())
        val properties = mutableMapOf<String, Property>()

        propertiesObj.forEach { (key, value) ->
            val propObj = value.jsonObject
            properties[key] = Property(
                type = propObj["type"]?.jsonPrimitive?.content ?: "",
                description = propObj["description"]?.jsonPrimitive?.content ?: ""
            )
        }

        val required = paramsObj["required"]
            ?.jsonArray?.mapNotNull { it.jsonPrimitive.content }
            ?: emptyList()

        return Parameters(
            type = paramsObj["type"]?.jsonPrimitive?.content ?: "",
            properties = properties,
            required = required
        )
    }

    suspend fun callTool(toolName: String, params: Map<String, String>): JsonElement? {
        return try {
            val queryString = params.map { "${it.key}=${it.value}" }.joinToString("&")
            val url = "$MCP_SERVER_URL/$toolName?$queryString"

            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build()

            val response = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()

            if (response.statusCode() == 200) {
                Json.parseToJsonElement(response.body())
            } else {
                println("Ошибка при вызове инструмента $toolName: HTTP ${response.statusCode()}")
                null
            }
        } catch (e: Exception) {
            println("Ошибка при вызове инструмента $toolName: ${e.message}")
            null
        }
    }



    fun connectWebSocket(webSocketListener: Listener): WebSocket? {
        return try {
            val client = HttpClient.newHttpClient()
            client.newWebSocketBuilder()
                .buildAsync(URI.create("ws://localhost:8000/websocket"), webSocketListener)
                .join()
        } catch (e: Exception) {
            println("Ошибка при подключении к WebSocket: ${e.message}")
            null
        }
    }
}