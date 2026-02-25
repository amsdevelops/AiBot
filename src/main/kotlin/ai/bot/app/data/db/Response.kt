package ai.bot.app.data.db

import org.ktorm.entity.Entity
import org.ktorm.schema.Table
import org.ktorm.schema.long
import org.ktorm.schema.timestamp
import org.ktorm.schema.varchar
import java.time.Instant

interface Response : Entity<Response> {
    val id: Long
    val content: String
    val createdAt: Instant
    
    companion object : Entity.Factory<Response>()
}

object DbResponse : Table<Response>("responses") {
    val id = long("id").primaryKey().bindTo { it.id }
    val content = varchar("content").bindTo { it.content }
    val createdAt = timestamp("created_at").bindTo { it.createdAt }
}