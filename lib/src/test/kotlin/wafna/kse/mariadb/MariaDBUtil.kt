package wafna.kse.mariadb

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import kotlin.test.fail
import kotlinx.coroutines.runBlocking
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.utility.DockerImageName
import wafna.kse.update
import wafna.kse.withTransaction

/** Spins up a database container and loans a data source. */
fun withMariaDB(borrow: suspend (DataSource) -> Unit) {
    MariaDBContainer(DockerImageName.parse("mariadb:11"))
        .withDatabaseName("test")
        .withUsername("root")
        .withPassword("password")
        .withEnv("MARIADB_ROOT_PASSWORD", "password")
        ?.use { container ->
            container.start()
            val config =
                HikariConfig().apply {
                    jdbcUrl = container.jdbcUrl
                    username = "root"
                    password = "password"
                    driverClassName = container.driverClassName
                    maximumPoolSize = 32
                }
            runBlocking {
                val db = HikariDataSource(config)
                db.withTransaction {
                    update("CREATE SCHEMA IF NOT EXISTS kse")
                }
                borrow(db)
            }
        } ?: fail("Failed to create container.")
}
