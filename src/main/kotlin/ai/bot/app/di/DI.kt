package ai.bot.app.di

import ai.bot.app.TelegramBot
import ai.bot.app.data.repository.ResponsesRepository
import ai.bot.app.usecase.AddSavedResponsesToRequestUseCase
import ai.bot.app.usecase.ClearResponsesRepositoryUseCase
import ai.bot.app.usecase.GetLocalPropertiesUseCase
import ai.bot.app.usecase.RemoveLastResponseUseCase
import ai.bot.app.usecase.SaveResponseTextUseCase

object DI {
    private val repository: ResponsesRepository by lazy { ResponsesRepository() }
    private val saveUseCase: SaveResponseTextUseCase by lazy { SaveResponseTextUseCase(repository) }
    private val addResponsesUseCase: AddSavedResponsesToRequestUseCase by lazy {
        AddSavedResponsesToRequestUseCase(
            repository
        )
    }
    private val clearResponsesRepositoryUseCase: ClearResponsesRepositoryUseCase by lazy {
        ClearResponsesRepositoryUseCase(
            repository
        )
    }
    private val removeLastResponseUseCase: RemoveLastResponseUseCase by lazy {
        RemoveLastResponseUseCase(repository)
    }

    val telegramBot: TelegramBot? by lazy {
        GetLocalPropertiesUseCase("BOT_KEY")?.let { key ->
            TelegramBot(
                saveResponseTextUseCase = saveUseCase,
                addResponsesToRequestUseCase = addResponsesUseCase,
                clearResponsesRepositoryUseCase = clearResponsesRepositoryUseCase,
                removeLastResponseUseCase = removeLastResponseUseCase,
                botToken = key
            )
        }
    }
}