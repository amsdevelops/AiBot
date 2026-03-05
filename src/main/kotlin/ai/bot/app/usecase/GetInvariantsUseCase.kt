package ai.bot.app.usecase

import ai.bot.app.data.model.Invariants
import com.google.gson.Gson
import java.io.File

object GetInvariantsUseCase {
    operator fun invoke(): Invariants {
        val json = File("src/main/resources/invariants.json").readText()
        val gson = Gson()
        return gson.fromJson(json, Invariants::class.java)
    }
}