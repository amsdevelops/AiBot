package ai.bot.app.data.repository

import ai.bot.app.data.db.DbResponse
import org.ktorm.database.Database
import org.ktorm.dsl.delete
import org.ktorm.dsl.deleteAll
import org.ktorm.dsl.eq
import org.ktorm.dsl.inList
import org.ktorm.dsl.insert
import org.ktorm.entity.count
import org.ktorm.entity.filter
import org.ktorm.entity.map
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.sortedBy
import org.ktorm.entity.take
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

    fun count(): Int {
        return database.sequenceOf(responsesTable).count()
    }

    fun getOldRecords(limit: Int): String {
        return database.sequenceOf(responsesTable)
            .sortedBy { it.createdAt }
            .take(limit)
            .map { it.content }
            .joinToString("\n")
    }

    fun deleteOldRecords(limit: Int) {
        val oldRecords = database.sequenceOf(responsesTable)
            .sortedBy { it.createdAt }
            .take(limit)
            .map { it.id }

        database.delete(responsesTable) { it.id inList oldRecords }
    }

    fun addWithBranch(content: String, branch: String) {
        database.insert(responsesTable) {
            set(responsesTable.content, content)
            set(responsesTable.branch, branch)
            set(responsesTable.createdAt, Instant.now())
        }
    }

    fun getRecordsByBranch(branch: String): String {
        return database.sequenceOf(responsesTable)
            .filter { it.branch eq branch }
            .map { it.content }
            .joinToString("\n\n")
    }
}