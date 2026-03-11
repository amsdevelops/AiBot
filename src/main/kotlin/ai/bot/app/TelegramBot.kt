package ai.bot.app

import ai.bot.app.data.model.Profile
import ai.bot.app.data.model.ProfileType
import ai.bot.app.mcp.InvestmentAgentMcpClient
import ai.bot.app.mcp.WeatherMcpUseCase
import ai.bot.app.menu.StrategyMenu
import ai.bot.app.remote.model.Message
import ai.bot.app.remote.model.OpenAIResponse
import ai.bot.app.remote.model.TextContent
import ai.bot.app.remote.model.ToolCall
import ai.bot.app.remote.model.parseArguments
import ai.bot.app.remote.usecase.GetOpenAIResponseUseCase
import ai.bot.app.taskmachine.TaskStateMachine
import ai.bot.app.usecase.AddKeyDataToRequestUseCase
import ai.bot.app.usecase.AddPersonalizedDataToRequestUseCase
import ai.bot.app.usecase.AddPersonalizedDataUseCase
import ai.bot.app.usecase.CalculateCostUseCase
import ai.bot.app.usecase.CalculateResponseTimeUseCase
import ai.bot.app.usecase.ClearResponsesRepositoryUseCase
import ai.bot.app.usecase.GetBranchRecordsAndAddToRequestUseCase
import ai.bot.app.usecase.GetProfileUseCase
import ai.bot.app.usecase.SaveKeyDataFromResponseUseCase
import ai.bot.app.usecase.SaveMessageBranchingUseCase
import ai.bot.app.usecase.SaveMessageSlidingWindowUseCase
import ai.bot.app.usecase.SaveResponseTextUseCase
import ai.bot.app.usecase.SlidingWindowStrategyUseCase
import ai.bot.app.usecase.SummaryStrategyUseCase
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

class TelegramBot(
    private val saveResponseTextUseCase: SaveResponseTextUseCase,
    private val saveMessageSlidingWindowUseCase: SaveMessageSlidingWindowUseCase,
    private val clearResponsesRepositoryUseCase: ClearResponsesRepositoryUseCase,
    private val summaryStrategyUseCase: SummaryStrategyUseCase,
    private val slidingWindowStrategyUseCase: SlidingWindowStrategyUseCase,
    private val getBranchRecordsAndAddToRequestUseCase: GetBranchRecordsAndAddToRequestUseCase,
    private val saveMessageBranchingUseCase: SaveMessageBranchingUseCase,
    private val addKeyDataToRequestUseCase: AddKeyDataToRequestUseCase,
    private val saveKeyDataFromResponseUseCase: SaveKeyDataFromResponseUseCase,
    private val addPersonalizedDataUseCase: AddPersonalizedDataUseCase,
    private val addPersonalizedDataToRequestUseCase: AddPersonalizedDataToRequestUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val taskStateMachine: TaskStateMachine,
    private val weatherMcpUseCase: WeatherMcpUseCase,
    private val investmentAgentMcpClient: InvestmentAgentMcpClient,
    botToken: String,
) : TelegramLongPollingBot(botToken) {

    private val strategyMenu = StrategyMenu()
    private var temperature: Double = 1.0
    private var selectedModel: String = "gpt-5.2"
    private var currentStrategy: String = "strategy_summary"
    private var isWaitingForPersonalizedData: Boolean = false
    private var selectedProfileType: ProfileType = ProfileType.COMMON
    private var isStateMachine: Boolean = false

    @OptIn(DelicateCoroutinesApi::class)
    override fun onUpdateReceived(update: Update?) {
        if (update == null) return
        if (update.hasMessage() && update.message.hasText()) {
            val message = update.message
            val chatId = message.chatId
            val text = message.text

            when (text) {
                "/clear" -> {
                    GlobalScope.launch { clearResponsesRepositoryUseCase() }
                    sendPlainTextMessage(chatId, "Чат очищен")
                }
                "/temperature" -> sendChatMessage(chatId)
                "/model" -> sendModelSelectionMessage(chatId)
                "/strategy" -> sendStrategyMenu(chatId)
                "/personalized" -> {
                    isWaitingForPersonalizedData = true
                    sendPersonalizedTemplate(chatId)
                }
                "/statemachine" -> isStateMachine = !isStateMachine
                "/profile" -> sendProfileMenu(chatId)
                "/weather" -> sendWeatherMcpMessage(chatId)

                "0.7", "1.0", "1.2" -> handleTemperatureButton(chatId, text)
                "gpt-3.5-turbo", "gpt-4o", "gpt-5.2" -> handleModelSelection(chatId, text)
                else -> {
                    if (isWaitingForPersonalizedData && text.contains("style:") || text.contains("constraints:") || text.contains(
                            "context:"
                        )
                    ) {
                        isWaitingForPersonalizedData = false
                        handlePersonalizedData(chatId, text)
                    } else {
                        GlobalScope.launch {

                            if (isStateMachine) {
                                sendTextMessage(chatId, taskStateMachine.processUserInput(text))
                            } else {val profile = getProfileUseCase(selectedProfileType)
                                val personalizedInput = addPersonalizedDataToRequestUseCase(text)
                                val finalInput = when (currentStrategy) {
                                    "strategy_summary" -> summaryStrategyUseCase(text)
                                    "strategy_sliding_window" -> slidingWindowStrategyUseCase(text)
                                    "strategy_sticky_facts" -> addKeyDataToRequestUseCase(text)
                                    "strategy_branching" -> {
                                        val branch = text.split(" ").firstOrNull() ?: "default"
                                        getBranchRecordsAndAddToRequestUseCase(branch, text)
                                    }

                                    else -> personalizedInput
                                }
                                val inputWithProfile = buildProfilePrompt(profile, finalInput)

                                val tools = investmentAgentMcpClient.getTools()
                                val response = GetOpenAIResponseUseCase.invoke(
                                    input = inputWithProfile,
                                    temperature = temperature,
                                    model = selectedModel,
                                    tools = tools,
                                )
                                when (currentStrategy) {
                                    "strategy_summary" -> response.getOrNull()?.let { saveResponseTextUseCase(it, text) }
                                    "strategy_sliding_window" -> response.getOrNull()
                                        ?.let { saveMessageSlidingWindowUseCase(it, text) }

                                    "strategy_sticky_facts" -> response.getOrNull()
                                        ?.let { saveKeyDataFromResponseUseCase(it, text) }

                                    "strategy_branching" -> response.getOrNull()?.let {
                                        val branch = text.split(" ").firstOrNull() ?: "default"
                                        saveMessageBranchingUseCase(it, branch, text)
                                    }
                                }
                                when {
                                    response.isSuccess -> {
                                        when (val output = response.getOrNull()?.output?.firstOrNull()) {
                                            is TextContent -> sendTextMessage(chatId, getContent(response))
                                            is ToolCall -> {
                                                if (output.name == "get_stock_price") {
                                                    val text = investmentAgentMcpClient.callTool(
                                                        toolName = output.name,
                                                        params = output.arguments.parseArguments()
                                                    )
                                                    val response = mapOf(
                                                        "type" to "function_call_output",
                                                        "call_id" to output.call_id,
                                                        "output" to Json.encodeToString(mapOf("stock_price" to text.toString()))
                                                    )
                                                    val mcpResponse = Json.encodeToString(response)
                                                    GetOpenAIResponseUseCase.invoke(
                                                        input = mcpResponse,
                                                        temperature = temperature,
                                                        model = selectedModel,
                                                    ).let { sendTextMessage(chatId, getContent(it)) }
                                                }
                                            }
                                            else -> { throw IllegalStateException("Unknown output type ${output?.javaClass}") }
                                        }
                                    }
                                    response.isFailure -> sendPlainTextMessage(
                                        chatId,
                                        response.exceptionOrNull()?.message ?: ""
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else if (update.hasCallbackQuery()) {
            val callbackQuery = update.callbackQuery
            val callbackData = callbackQuery.data
            val chatId = callbackQuery.message.chatId

            when (callbackData) {
                "strategy_summary", "strategy_sliding_window", "strategy_sticky_facts", "strategy_branching" -> {
                    currentStrategy = callbackData
                    val strategy = callbackData.removePrefix("strategy_")
                    sendPlainTextMessage(chatId, "Выбрана стратегия: $strategy")
                    // Здесь можно добавить логику обработки выбранной стратегии
                }

                "like" -> {
                    sendPlainTextMessage(chatId, "Спасибо за лайк!")
                }

                "dislike" -> {
                    sendPlainTextMessage(chatId, "Спасибо за обратную связь!")
                }

                "profile_COMMON", "profile_ANALYTIC", "profile_DEVELOPER" -> {
                    selectedProfileType = ProfileType.valueOf(callbackData.removePrefix("profile_"))
                    // Здесь можно сохранить выбранный профиль в состоянии бота или пользователе
                    sendPlainTextMessage(chatId, "Профиль ${selectedProfileType.name} выбран")
                }
            }
        }
    }

    private fun sendWeatherMcpMessage(chatId: Long) {
        GlobalScope.launch {
            val weather = weatherMcpUseCase.getServerCapabilities()
            sendPlainTextMessage(chatId, weather.toString())
        }
    }

    private fun buildProfilePrompt(profile: Profile, userInput: String): String {
        val profilePrompt = """
        You are an assistant with the following characteristics:
        Style: ${profile.style}
        Format: ${profile.format}
        Constraints: ${profile.constraints}
        
        User input: $userInput
    """.trimIndent()

        return profilePrompt
    }

    private fun sendStrategyMenu(chatId: Long) {
        strategyMenu.sendStrategyMenu(chatId, this)
    }

    private fun getContent(response: Result<OpenAIResponse>): String {
        val responseValue = response.getOrNull() ?: return ""
        val usageInfo =
            "Usage: ${responseValue.usage.inputTokens} input tokens, ${responseValue.usage.outputTokens} output tokens"
        val (inputCost, outputCost) = CalculateCostUseCase(responseValue.usage, selectedModel)
        val inputCostInfo = "Input cost: ${String.format("%.4f", inputCost)} RUB"
        val outputCostInfo = "Output cost: ${String.format("%.4f", outputCost)} RUB"
        val totalCostInfo = "Total cost: ${String.format("%.4f", inputCost + outputCost)} RUB"
        val responseTime = CalculateResponseTimeUseCase(responseValue)
        val responseTimeInfo = "Response time: $responseTime"
        val modelInfo = "Model: $selectedModel"
        val content = when (val output = responseValue.output.firstOrNull()) {
            is TextContent -> output.content.firstOrNull()?.text ?: ""
            is Message -> output.content.firstOrNull()?.text ?: ""
            else -> ""
        }

        return "$modelInfo\n\n$usageInfo\n\n$inputCostInfo\n$outputCostInfo\n\n$totalCostInfo\n\n$responseTimeInfo\n\n$content"
    }

    private fun sendPersonalizedTemplate(chatId: Long) {
        val template = """
            Отправьте персонализированные данные в следующем формате:
            
            /personalized
            style: [список стилей через запятую]
            constraints: [список ограничений через запятую]
            context: [список контекста через запятую]
            
            Пример:
            /personalized
            style: формальный, научный
            constraints: не использовать сленг, не использовать эмодзи
            context: описания технологических процессов
        """.trimIndent()

        sendPlainTextMessage(chatId, template)
    }

    private fun handlePersonalizedData(chatId: Long, text: String) {
        val lines = text.lines().filter { it.isNotBlank() }
        val parsedData = mutableMapOf<String, List<String>>()

        for (line in lines) {
            when {
                line.startsWith("style:") -> {
                    parsedData["style"] =
                        line.substringAfter(":").trim().split(",").map { it.trim() }.filter { it.isNotEmpty() }
                }

                line.startsWith("constraints:") -> {
                    parsedData["constraints"] =
                        line.substringAfter(":").trim().split(",").map { it.trim() }.filter { it.isNotEmpty() }
                }

                line.startsWith("context:") -> {
                    parsedData["context"] =
                        line.substringAfter(":").trim().split(",").map { it.trim() }.filter { it.isNotEmpty() }
                }
            }
        }

        // Сохраняем данные через юзкейс
        GlobalScope.launch {
            addPersonalizedDataUseCase(
                parsedData["style"] ?: emptyList(),
                parsedData["constraints"] ?: emptyList(),
                parsedData["context"] ?: emptyList()
            )
            sendPlainTextMessage(
                chatId,
                "Персонализированные данные сохранены! Теперь все запросы будут использовать эти параметры."
            )
        }
    }

    private fun sendTextMessage(chatId: Long, text: String) {
        val messages = splitText(text, 4096)
        messages.forEachIndexed { index, messageText ->
            val message = SendMessage().apply {
                this.chatId = chatId.toString()
                this.text = messageText
                // Добавим кнопки только к последнему сообщению
                if (index == messages.size - 1) {
                    this.replyMarkup = InlineKeyboardMarkup().apply {
                        keyboard = listOf(
                            listOf(
                                InlineKeyboardButton("👍").also { it.callbackData = "like" },
                                InlineKeyboardButton("👎").also { it.callbackData = "dislike" }
                            )
                        )
                    }
                }
            }

            try {
                execute(message)
            } catch (e: TelegramApiException) {
                e.printStackTrace()
            }
        }
    }

    private fun splitText(text: String, chunkSize: Int): List<String> {
        if (text.length <= chunkSize) {
            return listOf(text)
        }
        val chunks = mutableListOf<String>()
        var startIndex = 0
        while (startIndex < text.length) {
            val endIndex = minOf(startIndex + chunkSize, text.length)
            chunks.add(text.substring(startIndex, endIndex))
            startIndex = endIndex
        }
        return chunks
    }

    private fun sendPlainTextMessage(chatId: Long, text: String) {
        val message = SendMessage().apply {
            this.chatId = chatId.toString()
            this.text = text
        }

        try {
            execute(message)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }

    private fun sendChatMessage(chatId: Long) {
        val message = SendMessage().apply {
            this.chatId = chatId.toString()
            this.text = "Выберите действие:"
            this.replyMarkup = ReplyKeyboardMarkup().apply {
                keyboard = listOf(
                    KeyboardRow().apply {
                        add(KeyboardButton("0.7"))
                        add(KeyboardButton("1.0"))
                        add(KeyboardButton("1.2"))
                    }
                )
            }
        }

        try {
            execute(message)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }

    private fun sendModelSelectionMessage(chatId: Long) {
        val message = SendMessage().apply {
            this.chatId = chatId.toString()
            this.text = "Выберите модель:"
            this.replyMarkup = ReplyKeyboardMarkup().apply {
                keyboard = listOf(
                    KeyboardRow().apply {
                        add(KeyboardButton("gpt-3.5-turbo"))
                        add(KeyboardButton("gpt-4o"))
                        add(KeyboardButton("gpt-5.2"))
                    }
                )
            }
        }

        try {
            execute(message)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }

    private fun handleTemperatureButton(chatId: Long, text: String) {
        when (text) {
            "0.7" -> {
                temperature = 0.7
                sendPlainTextMessage(chatId, "Temperature set to 0.7")
            }

            "1.0" -> {
                temperature = 1.0
                sendPlainTextMessage(chatId, "Temperature set to 1.0")
            }

            "1.2" -> {
                temperature = 1.2
                sendPlainTextMessage(chatId, "Temperature set to 1.2")
            }
        }
    }

    private fun sendProfileMenu(chatId: Long) {
        val keyboard = InlineKeyboardMarkup().apply {
            keyboard = listOf(
                listOf(
                    InlineKeyboardButton("COMMON").apply { callbackData = "profile_COMMON" },
                    InlineKeyboardButton("ANALYTIC").apply { callbackData = "profile_ANALYTIC" }
                ),
                listOf(
                    InlineKeyboardButton("DEVELOPER").apply { callbackData = "profile_DEVELOPER" }
                )
            )
        }

        val message = SendMessage().apply {
            this.chatId = chatId.toString()
            text = "Выберите профиль:"
            replyMarkup = keyboard
        }

        try {
            execute(message)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }

    private fun handleModelSelection(chatId: Long, text: String) {
        selectedModel = text
        sendPlainTextMessage(chatId, "Model selected: $selectedModel")
    }

    override fun getBotUsername(): String = "aibot"
}