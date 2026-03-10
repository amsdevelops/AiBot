package ai.bot.app.mcp

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class WeatherMcpUseCase(
    private val client: WeatherMcpClient
) {
    init {
        GlobalScope.launch {
            client.connect()
        }
    }

    suspend fun getCurrentWeather(latitude: Double, longitude: Double): String? {
        return client.callTool(
            toolName = "get_current_weather",
            arguments = mapOf(
                "latitude" to latitude,
                "longitude" to longitude
            )
        )?.content?.joinToString { it.type }
    }

    suspend fun getWeatherForecast(latitude: Double, longitude: Double, days: Int = 7): String? {
        return client.callTool(
            toolName = "get_forecast",
            arguments = mapOf(
                "latitude" to latitude,
                "longitude" to longitude,
                "days" to days
            )
        )?.content?.joinToString { it.type }
    }

    suspend fun getWeatherAlerts(state: String): String? {
        return client.callTool(
            toolName = "get_alerts",
            arguments = mapOf("state" to state)
        )?.content?.joinToString { it.type }
    }

    suspend fun checkServiceStatus(): String? {
        return client.callTool(
            toolName = "check_service_status",
            arguments = emptyMap()
        )?.content?.joinToString { it.type }
    }

    suspend fun getServerCapabilities(): String? {
        return client.getServerCapabilities()
    }
}