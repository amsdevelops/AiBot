package ai.bot.app

import ai.bot.app.usecase.GetLocalPropertiesUseCase
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

object Application {
    @JvmStatic
    fun main(args: Array<String>) {
        GetLocalPropertiesUseCase("BOT_KEY")?.let { key ->
            TelegramBotsApi(DefaultBotSession::class.java).registerBot(TelegramBot(key))
        }
    }
}