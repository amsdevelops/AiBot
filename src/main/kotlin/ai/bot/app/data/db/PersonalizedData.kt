package ai.bot.app.data.db

import org.ktorm.entity.Entity
import org.ktorm.schema.Table
import org.ktorm.schema.varchar
import org.ktorm.schema.long
import org.ktorm.schema.timestamp
import java.time.Instant

interface PersonalizedData : Entity<PersonalizedData> {
    val id: Long
    val style: String?
    val constraints: String?
    val context: String?
    val createdAt: Instant

    companion object : Entity.Factory<PersonalizedData>()
}

object DbPersonalizedData : Table<PersonalizedData>("personalized_data") {
    val id = long("id").primaryKey().bindTo { it.id }
    val style = varchar("style").bindTo { it.style ?: "" }
    val constraints = varchar("constraints").bindTo { it.constraints ?: "" }
    val context = varchar("context").bindTo { it.context ?: "" }
    val createdAt = timestamp("created_at").bindTo { it.createdAt }
}