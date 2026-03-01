package ai.bot.app.data.repository

import ai.bot.app.data.db.DbKeyData
import org.ktorm.database.Database
import org.ktorm.dsl.deleteAll
import org.ktorm.dsl.insert
import org.ktorm.entity.map
import org.ktorm.entity.sequenceOf
import java.time.Instant

class KeyDataRepository(
    private val database: Database
) {
    private val keyDataTable = DbKeyData

    fun add(content: String) {
        database.insert(keyDataTable) {
            set(keyDataTable.content, content)
            set(keyDataTable.createdAt, Instant.now())
        }
    }

    fun getAll(): String {
        return database.sequenceOf(keyDataTable)
            .map { it.content }
            .joinToString("\n\n")
    }

    fun clear() {
        database.deleteAll(keyDataTable)
    }
}