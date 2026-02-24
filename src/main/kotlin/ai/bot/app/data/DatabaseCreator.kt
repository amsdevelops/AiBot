package ai.bot.app.data

import ai.bot.app.usecase.GetLocalPropertiesUseCase
import org.ktorm.database.Database
import java.sql.Connection
import java.sql.DriverManager

object DatabaseCreator {
    
    private const val DB_URL = "jdbc:mysql://localhost:3306/"
    private const val DB_NAME = "DB_NAME"
    private const val DB_USER_NAME = "DB_USER_NAME"
    private const val DB_PASSWORD = "DB_PASSWORD"

    fun createDatabaseIfNotExists(): Database {
        // Создаем соединение с MySQL без указания конкретной базы
        val connection = DriverManager.getConnection(
            DB_URL,
            GetLocalPropertiesUseCase(DB_USER_NAME),
            GetLocalPropertiesUseCase(DB_PASSWORD)
        )

        connection.use { connection ->
            // Создаем базу данных, если она не существует
            connection.createStatement().use { stmt ->
                stmt.execute("CREATE DATABASE IF NOT EXISTS ${GetLocalPropertiesUseCase(DB_NAME)}")
                stmt.execute("USE ${GetLocalPropertiesUseCase(DB_NAME)}")
            }

            // Создаем таблицу responses
            createResponsesTable(connection)
        }

        return Database.connect(
            url = "${DB_URL}${GetLocalPropertiesUseCase(DB_NAME)}",
            user = GetLocalPropertiesUseCase(DB_USER_NAME),
            password = GetLocalPropertiesUseCase(DB_PASSWORD)
        )
    }
    
    private fun createResponsesTable(connection: Connection) {
        val createTableSql = """
            CREATE TABLE IF NOT EXISTS responses (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                content TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent()
        
        connection.createStatement().use { stmt ->
            stmt.execute(createTableSql)
        }
    }
}
