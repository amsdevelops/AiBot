package ai.bot.app.usecase

import ai.bot.app.data.model.Profile
import ai.bot.app.data.model.ProfileType
import kotlinx.serialization.json.Json

class LoadProfileFromFileUseCase {
    operator fun invoke(profileType: ProfileType): Profile {
        // Используем ClassLoader для чтения ресурсов
        val resource = javaClass.classLoader.getResource("profile/${profileType.name.lowercase()}.json")
            ?: throw IllegalArgumentException("Resource not found: profile/${profileType.name.lowercase()}.json")
        val json = resource.readText()
        return fromJson(json)
    }


    fun fromJson(json: String): Profile {
        val profile = Json.decodeFromString<Profile>(json)
        return profile
    }
}