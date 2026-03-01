package ai.bot.app.usecase

import ai.bot.app.data.repository.ResponsesRepository

class GetBranchRecordsAndAddToRequestUseCase(
    private val repository: ResponsesRepository
) {
    operator fun invoke(branch: String, input: String): String {
        val records = repository.getRecordsByBranch(branch)
        return if (records.isNotEmpty()) {
            "$records\n\n$input"
        } else {
            input
        }
    }
}
