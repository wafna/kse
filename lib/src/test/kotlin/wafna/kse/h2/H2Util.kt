package wafna.kse.h2

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import kotlinx.coroutines.runBlocking
import wafna.kse.update
import wafna.kse.withTransaction

/** Spins up a database container and loans a data source. */
internal fun withH2DB(f: suspend (DataSource) -> Unit) {
    val db = HikariDataSource(HikariConfig().apply {
//        jdbcUrl = "jdbc:h2:mem:test_db;DB_CLOSE_DELAY=-1"
        jdbcUrl = "jdbc:h2:mem:test_db"
        username = "sa"
        password = ""
        maximumPoolSize = 1
    })
    runBlocking {
        db.withTransaction {
            update("CREATE SCHEMA IF NOT EXISTS kse")
        }
        f(db)
    }
}

