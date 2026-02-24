package ai.bot.app

import ai.bot.app.di.DI
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

object Application {

    @JvmStatic
    fun main(args: Array<String>) {
        DI.telegramBot?.let { bot ->
            TelegramBotsApi(DefaultBotSession::class.java).registerBot(bot)
        }
    }
}