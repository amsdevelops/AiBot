package ai.bot.app.menu

import ai.bot.app.TelegramBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

class StrategyMenu(
) {
    fun sendStrategyMenu(chatId: Long, bot: TelegramBot) {
        val message = SendMessage().apply {
            this.chatId = chatId.toString()
            this.text = "Выберите стратегию:"
            this.replyMarkup = InlineKeyboardMarkup().apply {
                keyboard = listOf(
                    listOf(
                        InlineKeyboardButton("Summary").also { it.callbackData = "strategy_summary" },
                        InlineKeyboardButton("Sliding Window").also { it.callbackData = "strategy_sliding_window" }
                    ),
                    listOf(
                        InlineKeyboardButton("Sticky Facts").also { it.callbackData = "strategy_sticky_facts" },
                        InlineKeyboardButton("Branching").also { it.callbackData = "strategy_branching" }
                    )
                )
            }
        }

        try {
            bot.execute(message)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }
}