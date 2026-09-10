package wafna.kse.common

import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import wafna.kse.h2.User
import wafna.kse.insert
import wafna.kse.readRecords
import wafna.kse.requireInserts
import wafna.kse.requireUpdates
import wafna.kse.select
import wafna.kse.setInt
import wafna.kse.setString
import wafna.kse.update
import wafna.kse.withTransaction

suspend fun testDatabase(db: DataSource) {
    db.withTransaction {
        update("CREATE TABLE kse.users (id INT PRIMARY KEY, name VARCHAR(255))")
        val alice = User(1, "Alice")
        val bob = User(2, "Bob")
        val users = listOf(alice, bob)
        insert(
            "INSERT INTO kse.users (id, name) VALUES (?, ?)",
            // List and parameterize.
            users.map {
                listOf(it.id.setInt(), it.name.setString())
            }.iterator()
        ).requireInserts(users.size)
        select("SELECT id, name FROM kse.users") {
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
        update("UPDATE kse.users SET name = ? WHERE id = ?", newName.setString(), users[0].id.setInt())
            .requireUpdates(1)
        select("SELECT id, name FROM kse.users") {
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