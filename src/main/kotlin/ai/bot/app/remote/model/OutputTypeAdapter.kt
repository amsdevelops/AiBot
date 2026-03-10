package ai.bot.app.remote.model

import com.google.gson.*
import java.lang.reflect.Type

class OutputTypeAdapter : JsonDeserializer<List<Output>> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): List<Output> {
        val outputs = mutableListOf<Output>()
        
        if (json.isJsonArray) {
            json.asJsonArray.forEach { element ->
                if (element.isJsonObject) {
                    val jsonObject = element.asJsonObject
                    val type = jsonObject.get("type")?.asString
                    
                    val output = when (type) {
                        "text_content" -> {
                            context.deserialize<Output>(element, TextContent::class.java)
                        }
                        "function_call" -> {
                            context.deserialize<Output>(element, ToolCall::class.java)
                        }
                        "message" -> {
                            context.deserialize<Output>(element, Message::class.java)
                        }
                        else -> null
                    }
                    
                    output?.let { outputs.add(it) }
                }
            }
        }
        
        return outputs
    }
}