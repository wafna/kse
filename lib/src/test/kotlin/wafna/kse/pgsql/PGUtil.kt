package wafna.kse.pgsql

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import kotlin.test.fail
import kotlinx.coroutines.runBlocking
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import wafna.kse.update
import wafna.kse.withTransaction

/** Spins up a database container and loans a data source. */
fun withPGDB(borrow: suspend DataSource.() -> Unit) {
    PostgreSQLContainer(DockerImageName.parse("postgres:15-alpine"))
        .withDatabaseName("test")
        .withUsername("username")
        .withPassword("password")
        ?.use { container ->
            container.start()
            val config =
                HikariConfig().apply {
                    jdbcUrl = container.jdbcUrl
                    username = container.username
                    password = container.password
                    driverClassName = container.driverClassName
                    maximumPoolSize = 32
                }
            runBlocking {
                val db = HikariDataSource(config)
                db.withTransaction {
                    update("CREATE SCHEMA IF NOT EXISTS kse")
                }
                db.borrow()
            }
        } ?: fail("Failed to create container.")
}
