package ai.bot.app.taskmachine

import ai.bot.app.remote.usecase.GetOpenAIResponseUseCase
import ai.bot.app.usecase.GetInvariantsUseCase
import java.util.concurrent.atomic.AtomicReference

enum class TaskState {
    PLANNING,
    EXECUTION,
    VALIDATION,
    DONE
}

class TaskStateMachine {
    private val currentState = AtomicReference<TaskState>(TaskState.PLANNING)
    private val userQuestion = AtomicReference<String?>()
    private val plan = AtomicReference<String?>()
    private val executionResult = AtomicReference<String?>()
    private val validationResult = AtomicReference<String?>()
    private val temperature = 0.5
    private val selectedModel = "gpt-4o"

    suspend fun processUserInput(input: String): String {
        return when (currentState.get()) {
            TaskState.PLANNING -> processPlanning(input)
            TaskState.EXECUTION -> processExecution(input)
            TaskState.VALIDATION -> processValidation(input)
            TaskState.DONE -> processDone(input)
        }
    }

    private suspend fun processPlanning(input: String): String {
        // Сохраняем вопрос пользователя
        userQuestion.set(input)

        // Создаем промт для составления плана
        val prompt = """
               Проанализируйте следующий вопрос и создайте подробный план действий для его решения:
                "$input"
    
               Пожалуйста, предоставьте пошаговый план действий, которые необходимо предпринять для решения данного вопроса.
        """.trimIndent()

        // Получаем ответ от OpenAI
        val response = GetOpenAIResponseUseCase(
            input = prompt,
            temperature = temperature,
            model = selectedModel
        )

        return when {
            response.isSuccess -> {
                val planText = response.getOrNull()?.output
                    ?.firstOrNull { it.role == "assistant" }
                    ?.content
                    ?.firstOrNull()
                    ?.text
                    ?.takeIf { !it.isNullOrBlank() }

                if (planText != null) {
                    currentState.set(TaskState.EXECUTION)
                    plan.set(planText)
                    "План действий составлен:\n\n$planText\n\nПодтвердите, что этот план корректен и мы можем переходить к выполнению."
                } else {
                    "Не удалось получить план действий. Пожалуйста, повторите запрос."
                }
            }

            else -> {
                response.exceptionOrNull()?.message ?: "Ошибка при получении плана действий"
            }
        }
    }

    private suspend fun processExecution(input: String): String {
        // Если пользователь подтвердил план
        if (input.lowercase() in listOf("да", "yes", "подтверждаю", "верно")) {
            // Получаем план
            val currentPlan = plan.get() ?: return "План не найден. Повторите запрос."

            // Создаем промт для выполнения задачи
            val prompt = """
                Используя следующий план действий, выполните задачу:
    
                План: $currentPlan
    
                Вопрос: ${userQuestion.get()}
    
                Предоставьте подробный ответ на исходный вопрос на основе выполнения плана.
            """.trimIndent()

            // Получаем ответ от OpenAI
            val response = GetOpenAIResponseUseCase(
                input = prompt,
                temperature = temperature,
                model = selectedModel
            )

            return when {
                response.isSuccess -> {
                    val result = response.getOrNull()?.output
                        ?.firstOrNull { it.role == "assistant" }
                        ?.content
                        ?.firstOrNull()
                        ?.text
                        ?.takeIf { !it.isNullOrBlank() }

                    if (result != null) {
                        executionResult.set(result)
                        currentState.set(TaskState.VALIDATION)
                        "Задача выполнена. Результат:\n\n$result\n\nПроверим, соответствует ли ответ изначальному вопросу и плану."
                    } else {
                        "Не удалось получить результат выполнения задачи. Повторите попытку."
                    }
                }

                else -> {
                    response.exceptionOrNull()?.message ?: "Ошибка при выполнении задачи"
                }
            }
        } else {
            // Пользователь отклонил план, отправляем новый запрос
            return processPlanning(userQuestion.get() ?: "")
        }
    }

    private suspend fun processValidation(input: String): String {
        // Если пользователь подтвердил результат
        if (input.lowercase() in listOf("да", "yes", "подтверждаю", "верно")) {
            currentState.set(TaskState.DONE)
            return "Задача завершена успешно. Результат: ${executionResult.get()}"
        }

        // Если пользователь отклонил результат, повторяем выполнение
        val currentPlan = plan.get() ?: return "План не найден. Повторите запрос."
        val currentQuestion = userQuestion.get() ?: return "Вопрос не найден. Повторите запрос."

        // Загружаем инварианты
        val invariants = GetInvariantsUseCase()

        // Создаем промт для проверки соответствия инвариантам
        val prompt = """
        Проверьте, правильно ли следующий ответ отвечает на исходный вопрос и следует ли он плану действий:
        
        Исходный вопрос: $currentQuestion
        
        План действий: $currentPlan
        
        Ответ для проверки: ${executionResult.get()}
        
        Инварианты для проверки:
        - ${invariants.invariants}
        
        Является ли ответ правильным и полным? Ответьте "YES", если правильно, или "NO", если неправильно.
    """.trimIndent()

        // Получаем ответ от OpenAI
        val response = GetOpenAIResponseUseCase(
            input = prompt,
            temperature = 0.1, // Более строгий температурный параметр для проверки
            model = selectedModel
        )

        return when {
            response.isSuccess -> {
                val checkResult = response.getOrNull()?.output
                    ?.firstOrNull { it.role == "assistant" }
                    ?.content
                    ?.firstOrNull()
                    ?.text
                    ?.takeIf { !it.isNullOrBlank() }

                if (checkResult?.uppercase()?.contains("YES") == true) {
                    currentState.set(TaskState.DONE)
                    "Задача завершена успешно. Результат: ${executionResult.get()}"
                } else if (checkResult?.uppercase()?.contains("NO") == true) {
                    // Повторяем выполнение
                    val executionPrompt = """
                    Используя следующий план действий, выполните задачу:
                    
                    План: $currentPlan
                    
                    Вопрос: $currentQuestion
                    
                    Пожалуйста, следуйте следующим инвариантам:
                    - ${invariants.invariants}
                    
                    Предоставьте подробный ответ на исходный вопрос на основе выполнения плана.
                """.trimIndent()

                    val executionResponse = GetOpenAIResponseUseCase(
                        input = executionPrompt,
                        temperature = temperature,
                        model = selectedModel
                    )

                    when {
                        executionResponse.isSuccess -> {
                            val newResult = executionResponse.getOrNull()?.output
                                ?.firstOrNull { it.role == "assistant" }
                                ?.content
                                ?.firstOrNull()
                                ?.text
                                ?.takeIf { !it.isNullOrBlank() }

                            if (newResult != null) {
                                executionResult.set(newResult)
                                "Повторное выполнение задачи с учетом инвариантов. Результат:\n\n$newResult\n\nПроверим снова."
                            } else {
                                "Не удалось получить результат повторного выполнения. Повторите попытку."
                            }
                        }

                        else -> {
                            executionResponse.exceptionOrNull()?.message ?: "Ошибка при повторном выполнении"
                        }
                    }
                } else {
                    "Не удалось определить корректность ответа. Повторите попытку."
                }
            }

            else -> {
                response.exceptionOrNull()?.message ?: "Ошибка при проверке результата"
            }
        }
    }

    private fun processDone(input: String): String {
        // Сброс состояния для новой задачи
        resetState()
        return "Процесс завершен. Для новой задачи введите вопрос."
    }

    private fun resetState() {
        currentState.set(TaskState.PLANNING)
        userQuestion.set(null)
        plan.set(null)
        executionResult.set(null)
        validationResult.set(null)
    }

    fun getCurrentState(): TaskState = currentState.get()
    fun getUserQuestion(): String? = userQuestion.get()
    fun getPlan(): String? = plan.get()
    fun getExecutionResult(): String? = executionResult.get()
}