package ai.bot.app.data.repository

import ai.bot.app.data.db.DbResponse
import org.ktorm.database.Database
import org.ktorm.dsl.deleteAll
import org.ktorm.dsl.insert
import org.ktorm.entity.map
import org.ktorm.entity.sequenceOf
import java.time.Instant

class ResponsesRepository(
    private val database: Database
) {
    private val responsesTable = DbResponse

    fun add(content: String) {
        database.insert(responsesTable) {
            set(responsesTable.content, content)
            set(responsesTable.createdAt, Instant.now())
        }
    }

    fun getAll(): String {
        return database.sequenceOf(responsesTable).map { it.content }.joinToString()
    }

    fun clear() {
        database.deleteAll(responsesTable)
    }
}