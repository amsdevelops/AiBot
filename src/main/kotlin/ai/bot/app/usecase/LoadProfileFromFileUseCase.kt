package ai.bot.app.usecase

import ai.bot.app.data.model.Profile
import ai.bot.app.data.model.ProfileType
import com.google.gson.Gson
import java.io.File
import kotlin.jvm.java

class LoadProfileFromFileUseCase {
    operator fun invoke(profileType: ProfileType): Profile {
        val file = File("src/main/resources/profile/${profileType.fileName}")
        val content = file.readText()
        return Gson().fromJson(content, Profile::class.java)
    }
}