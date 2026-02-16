package ai.bot.app.usecase

import java.io.FileInputStream
import java.util.Properties

object GetLocalPropertiesUseCase {
    private const val DEFAULT_FILE_PATH = "local.properties"
    private val properties = Properties()

    operator fun invoke(key: String): String? =
        if (properties.isEmpty) {
            FileInputStream(DEFAULT_FILE_PATH).use {
                properties.load(it)
            }
            properties.getProperty(key)
        } else {
            properties.getProperty(key)
        }
}