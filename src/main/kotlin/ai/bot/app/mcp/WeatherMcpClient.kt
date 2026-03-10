package ai.bot.app.mcp

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResultBase
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.Implementation
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class WeatherMcpClient {
    private val client = Client(
        clientInfo = Implementation(
            name = "io.github.dgahagan/weather-mcp",
            version = "1.7.0"
        )
    )

    suspend fun connect() {
        val transport = StdioClientTransport(
            input = System.`in`,
            output = System.out
        )
        try {
            client.connect(transport)
            println("✅ Подключено к weather‑mcp серверу")
        } catch (e: Exception) {
            println("❌ Ошибка подключения: ${e.message}")
        }
    }

    suspend fun callTool(toolName: String, arguments: Map<String, Any>): CallToolResultBase? {
        val jsonArguments = buildJsonObject {
            arguments.forEach { (key, value) ->
                when (value) {
                    is String -> put(key, JsonPrimitive(value))
                    is Number -> put(key, JsonPrimitive(value))
                    is Boolean -> put(key, JsonPrimitive(value))
                    else -> put(key, JsonPrimitive(value.toString()))
                }
            }
        }

        val request = CallToolRequest(
            name = toolName,
            arguments = jsonArguments
        )
        return client.callTool(request)
    }

    suspend fun getServerCapabilities(): String? {
        return client.getServerVersion()?.toString()
    }

    suspend fun disconnect() {
        client.close()
        println("🔌 Отключено от weather‑mcp сервера")
    }
}