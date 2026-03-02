package ai.bot.app.di

import ai.bot.app.TelegramBot
import ai.bot.app.data.DatabaseCreator
import ai.bot.app.data.repository.KeyDataRepository
import ai.bot.app.data.repository.ResponsesRepository
import ai.bot.app.usecase.AddKeyDataToRequestUseCase
import ai.bot.app.usecase.AddSavedResponsesToRequestUseCase
import ai.bot.app.usecase.ClearResponsesRepositoryUseCase
import ai.bot.app.usecase.GetBranchRecordsAndAddToRequestUseCase
import ai.bot.app.usecase.GetLocalPropertiesUseCase
import ai.bot.app.usecase.SaveKeyDataFromResponseUseCase
import ai.bot.app.usecase.SaveMessageBranchingUseCase
import ai.bot.app.usecase.SaveMessageSlidingWindowUseCase
import ai.bot.app.usecase.SaveResponseTextUseCase
import ai.bot.app.usecase.SlidingWindowStrategyUseCase
import ai.bot.app.usecase.SummaryStrategyUseCase
import org.ktorm.database.Database

object DI {
    private val database: Database by lazy {
        DatabaseCreator.createDatabaseIfNotExists()
    }
    private val repository: ResponsesRepository by lazy { ResponsesRepository(database) }
    private val saveUseCase: SaveResponseTextUseCase by lazy { SaveResponseTextUseCase(repository) }
    private val addResponsesUseCase: AddSavedResponsesToRequestUseCase by lazy {
        AddSavedResponsesToRequestUseCase(
            repository
        )
    }
    private val clearResponsesRepositoryUseCase: ClearResponsesRepositoryUseCase by lazy {
        ClearResponsesRepositoryUseCase(
            repository,
            keyDataRepository,
        )
    }
    private val keyDataRepository: KeyDataRepository by lazy { KeyDataRepository(database) }
    private val saveKeyDataFromResponseUseCase: SaveKeyDataFromResponseUseCase by lazy {
        SaveKeyDataFromResponseUseCase(repository, keyDataRepository)
    }
    private val addKeyDataUseCase: AddKeyDataToRequestUseCase by lazy { AddKeyDataToRequestUseCase(keyDataRepository) }
    private val summaryStrategyUseCase: SummaryStrategyUseCase by lazy {
        SummaryStrategyUseCase(
            repository,
            addResponsesUseCase
        )
    }
    private val slidingWindowStrategyUseCase: SlidingWindowStrategyUseCase by lazy {
        SlidingWindowStrategyUseCase(addResponsesUseCase)
    }
    private val getBranchRecordsAndAddToRequestUseCase: GetBranchRecordsAndAddToRequestUseCase by lazy {
        GetBranchRecordsAndAddToRequestUseCase(repository)
    }
    private val saveMessageBranchingUseCase: SaveMessageBranchingUseCase by lazy {
        SaveMessageBranchingUseCase(repository)
    }
    private val saveMessageSlidingWindowUseCase: SaveMessageSlidingWindowUseCase by lazy {
        SaveMessageSlidingWindowUseCase(repository)
    }

    val telegramBot: TelegramBot? by lazy {
        GetLocalPropertiesUseCase("BOT_KEY")?.let { key ->
            TelegramBot(
                saveResponseTextUseCase = saveUseCase,
                summaryStrategyUseCase = summaryStrategyUseCase,
                clearResponsesRepositoryUseCase = clearResponsesRepositoryUseCase,
                slidingWindowStrategyUseCase = slidingWindowStrategyUseCase,
                addKeyDataToRequestUseCase = addKeyDataUseCase,
                saveKeyDataFromResponseUseCase = saveKeyDataFromResponseUseCase,
                getBranchRecordsAndAddToRequestUseCase = getBranchRecordsAndAddToRequestUseCase,
                saveMessageBranchingUseCase = saveMessageBranchingUseCase,
                saveMessageSlidingWindowUseCase = saveMessageSlidingWindowUseCase,
                botToken = key,
            )
        }
    }
}