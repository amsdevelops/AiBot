package ai.bot.app.data.repository

import ai.bot.app.data.db.DbPersonalizedData
import ai.bot.app.data.db.PersonalizedData
import org.ktorm.database.Database
import org.ktorm.dsl.deleteAll
import org.ktorm.dsl.insert
import org.ktorm.entity.firstOrNull
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.toList
import java.time.Instant

class PersonalizedDataRepository(
    private val database: Database
) {
    private val personalizedTable = DbPersonalizedData

    fun add(style: List<String>?, constraints: List<String>?, context: List<String>?) {
        database.deleteAll(personalizedTable)
        database.insert(personalizedTable) {
            set(personalizedTable.style, style?.joinToString(",") ?: "")
            set(personalizedTable.constraints, constraints?.joinToString(",") ?: "")
            set(personalizedTable.context, context?.joinToString(",") ?: "")
            set(personalizedTable.createdAt, Instant.now())
        }
    }

    fun get(): PersonalizedData? {
        return database.sequenceOf(personalizedTable).firstOrNull()
    }

    fun clear() {
        database.deleteAll(personalizedTable)
    }
}