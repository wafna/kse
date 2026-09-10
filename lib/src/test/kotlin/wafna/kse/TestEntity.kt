package wafna.kse

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.assertThrows
import java.sql.Connection

data class User(val id: Int, val name: String)

class TestEntity {
    @Test
    fun testEntity() {
        val userEntity = object : Entity<User>(
            table = Table(listOf("KSE"), "TEST_ENTITY"),
            fields = listOf("ID".field, "NAME".field)
        ) {
            override fun read(resultSet: ResultIterator): User =
                User(resultSet.getInt()!!, resultSet.getString()!!)

            context(cx: Connection)
            override fun write(record: User): List<Param> =
                listOf(record.id.setInt(), record.name.setString())
        }
        runTestDB { db ->
            db.withTransaction {
                update("CREATE TABLE IF NOT EXISTS kse.test_entity (id INT PRIMARY KEY, name VARCHAR(255))")
                val alice = User(1, "Alice")
                val bob = User(2, "Bob")
                val users = listOf(alice, bob)
                userEntity.insert(users).requireInserts(2)
                userEntity.select("U", "WHERE U.ID = ?", 1.setInt()).also {
                    assertEquals(1, it.size)
                    assertEquals(it[0], alice)
                }
                assertNotNull(
                    userEntity.select("U", "WHERE U.ID = ?", 2.setInt()).unique
                )
                assertThrows<IllegalStateException> {
                    userEntity.select("U", "WHERE U.ID = ?", 999.setInt()).unique
                }
                assertNotNull(
                    userEntity.select("U", "WHERE U.ID = ?", 2.setInt()).optional
                )
                assertNull(
                    userEntity.select("U", "WHERE U.ID = ?", (-1).setInt()).optional
                )
            }
        }
    }
    @Test
    fun testEntityNoSchema() {
        val userEntity = object : Entity<User>(
            table = Table("TEST_ENTITY"),
            fields = listOf("ID".field, "NAME".field)
        ) {
            override fun read(resultSet: ResultIterator): User =
                User(resultSet.getInt()!!, resultSet.getString()!!)

            context(cx: Connection)
            override fun write(record: User): List<Param> =
                listOf(record.id.setInt(), record.name.setString())
        }
        runTestDB { db ->
            db.withTransaction {
                update("CREATE TABLE IF NOT EXISTS test_entity (id INT PRIMARY KEY, name VARCHAR(255))")
                val alice = User(1, "Alice")
                val bob = User(2, "Bob")
                val users = listOf(alice, bob)
                userEntity.insert(users).requireInserts(2)
                userEntity.select("U", "WHERE U.ID = ?", 1.setInt()).also {
                    assertEquals(1, it.size)
                    assertEquals(it[0], alice)
                }
                assertNotNull(
                    userEntity.select("U", "WHERE U.ID = ?", 2.setInt()).unique
                )
                assertThrows<IllegalStateException> {
                    userEntity.select("U", "WHERE U.ID = ?", 999.setInt()).unique
                }
                assertNotNull(
                    userEntity.select("U", "WHERE U.ID = ?", 2.setInt()).optional
                )
                assertNull(
                    userEntity.select("U", "WHERE U.ID = ?", (-1).setInt()).optional
                )
            }
        }
    }
}