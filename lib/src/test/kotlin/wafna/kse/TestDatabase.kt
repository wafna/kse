package wafna.kse

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.time.toKotlinInstant
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month

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
                        listOf(it.id.setInt(), it.name.setString())
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
                update("UPDATE users SET name = ? WHERE id = ?", newName.setString(), users[0].id.setInt())
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
    @Test
    fun testSetters() {
        runTestDB { db ->
            db.withTransaction {
                update(
                    """
                    CREATE TABLE IF NOT EXISTS all_types (
                        id INT PRIMARY KEY,
                        c_int INT,
                        c_double DOUBLE PRECISION,
                        c_string VARCHAR(255),
                        c_instant TIMESTAMP,
                        c_boolean BOOLEAN,
                        c_localdate DATE,
                        c_object VARCHAR(255),
                        c_array INT ARRAY
                    )
                    """.trimIndent()
                )

                val nowInstant = Instant.fromEpochMilliseconds(1700000000000L)
                val testDate = LocalDate(2026, Month.SEPTEMBER, 9)
                val testArray = listOf(10, 20, 30)

                // Test non-null setters
                update(
                    """
                    INSERT INTO all_types (
                        id, c_int, c_double, c_string, c_instant, c_boolean, c_localdate, c_object, c_array
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    1.setInt(),
                    42.setInt(),
                    3.14.setDouble(),
                    "hello".setString(),
                    nowInstant.setInstant(),
                    true.setBoolean(),
                    testDate.setLocalDate(),
                    "custom_object".setObject(),
                    testArray.setArray("INTEGER")
                ).requireUpdates(1)

                select("SELECT * FROM all_types WHERE id = ?", 1.setInt()) {
                    readRecords {
                        assertEquals(1, getInt())
                        assertEquals(42, getInt())
                        assertEquals(3.14, getDouble())
                        assertEquals("hello", getString())
                        assertEquals(nowInstant, getTimestamp()?.toInstant()?.toKotlinInstant())
                        assertEquals(true, getBoolean())
                        assertEquals(testDate, getLocalDate())
                        assertEquals("custom_object", getObject())
                        val arrayResult = (getArray()?.array as? Array<*>)?.toList()
                        assertEquals(testArray, arrayResult)
                    }
                }

                // Test null setters and paramNull
                val nullInt: Int? = null
                val nullDouble: Double? = null
                val nullString: String? = null
                val nullInstant: Instant? = null
                val nullBoolean: Boolean? = null
                val nullLocalDate: LocalDate? = null
                val nullObject: Any? = null

                update(
                    """
                    INSERT INTO all_types (
                        id, c_int, c_double, c_string, c_instant, c_boolean, c_localdate, c_object, c_array
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    2.setInt(),
                    nullInt.setInt(),
                    nullDouble.setDouble(),
                    nullString.setString(),
                    nullInstant.setInstant(),
                    nullBoolean.setBoolean(),
                    nullLocalDate.setLocalDate(),
                    nullObject.setObject(),
                    paramNull
                ).requireUpdates(1)

                select("SELECT * FROM all_types WHERE id = ?", 2.setInt()) {
                    readRecords {
                        assertEquals(2, getInt())
                        assertNull(getInt())
                        assertNull(getDouble())
                        assertNull(getString())
                        assertNull(getTimestamp())
                        assertNull(getBoolean())
                        assertNull(getLocalDate())
                        assertNull(getObject())
                        assertNull(getArray())
                    }
                }
            }
        }
    }
}