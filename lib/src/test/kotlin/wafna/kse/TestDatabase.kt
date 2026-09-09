package wafna.kse

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class TestDatabase {
    @Test
    fun test() {
        val db = HikariDataSource(HikariConfig().also {
            it.jdbcUrl = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1"
            it.username = "sa"
            it.password = ""
            it.maximumPoolSize = 1
        })

        runBlocking {
            db.withTransaction {
                update("CREATE TABLE IF NOT EXISTS test (id INT PRIMARY KEY, name VARCHAR(255))")
                val items = listOf(
                    1 to "one",
                    2 to "two"
                )
                insert(
                    "INSERT INTO test (id, name) VALUES (?, ?)",
                    // List and parameterize.
                    items.map {
                        listOf(it.first.paramInt(), it.second.paramString())
                    }.iterator()
                ).requireInserts(items.size)
                select("SELECT id, name FROM test") {
                    readRecords {
                        val id = getInt()
                        val name = getString()
                        id to name
                    }
                }.also {
                    assertEquals(items.size, it.size)
                    it.forEach { (id, name) ->
                        items.any { it.first == id && it.second == name }
                    }
                }
                update("UPDATE test SET name = ? WHERE id = ?", "Uno".paramString(), items[0].first.paramInt())
                    .requireUpdates(1)
                select("SELECT id, name FROM test") {
                    readRecords {
                        val id = getInt()
                        val name = getString()
                        id to name
                    }
                }.also {
                    assertEquals(items.size, it.size)
                    assertTrue(it.any { it.first == items[0].first && it.second == "Uno" })
                    assertTrue(it.any { it.first == items[1].first && it.second == items[1].second })
                }
            }
        }
    }
}