package wafna.kse

import kotlin.test.Test
import kotlin.test.assertEquals
import java.sql.Connection

data class User(val id: Int, val name: String)

val userEntity  = object: Entity<User>(
    table = Table("TEST_ENTITY"),
    fields = listOf("ID".field, "NAME".field)
) {
    override fun read(resultSet: ResultIterator): User =
        User(resultSet.getInt()!!, resultSet.getString()!!)

    context(cx: Connection)
    override fun write(record: User): List<Param> =
        listOf(record.id.setInt(), record.name.setString())
}

class TestEntity {
    @Test
    fun foo() {
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
            }
        }
    }
}