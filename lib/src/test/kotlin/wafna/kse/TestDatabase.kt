package wafna.kse

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

fun runTestDB(f: suspend (DataSource) -> Unit) {
    val db = HikariDataSource(HikariConfig().also {
        it.jdbcUrl = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1"
        it.username = "sa"
        it.password = ""
        it.maximumPoolSize = 1
    })

    runBlocking { f(db) }
}

class TestDatabase {
    @Test
    fun test() {
        runTestDB { db ->
            db.withTransaction {
                update("CREATE TABLE IF NOT EXISTS users (id INT PRIMARY KEY, name VARCHAR(255))")
                val alice = User(1, "Alice")
                val bob = User(2, "Bob")
                val users = listOf(alice, bob)
                insert(
                    "INSERT INTO users (id, name) VALUES (?, ?)",
                    // List and parameterize.
                    users.map {
                        listOf(it.id.paramInt(), it.name.paramString())
                    }.iterator()
                ).requireInserts(users.size)
                select("SELECT id, name FROM users") {
                    readRecords {
                        val id = getInt()
                        val name = getString()
                        id to name
                    }
                }.also {
                    assertEquals(users.size, it.size)
                    it.forEach { (id, name) ->
                        users.any { it.id == id && it.name == name }
                    }
                }
                val newName = "Carol"
                update("UPDATE users SET name = ? WHERE id = ?", newName.paramString(), users[0].id.paramInt())
                    .requireUpdates(1)
                select("SELECT id, name FROM users") {
                    readRecords {
                        val id = getInt()
                        val name = getString()
                        id to name
                    }
                }.also {
                    assertEquals(users.size, it.size)
                    assertTrue(it.any { it.first == users[0].id && it.second == newName })
                    assertTrue(it.any { it.first == users[1].id && it.second == users[1].name })
                }
            }
        }
    }
}