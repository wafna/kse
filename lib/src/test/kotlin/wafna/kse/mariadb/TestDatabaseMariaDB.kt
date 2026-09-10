package wafna.kse.mariadb

import kotlin.test.Test
import kotlin.test.assertEquals
import wafna.kse.common.testDataSource
import wafna.kse.quoteIdentifiers
import wafna.kse.withTransaction

class TestDatabaseMariaDB {
    @Test
    fun testQuoteIdentifier() {
        withMariaDB { db ->
            db.withTransaction {
                assertEquals(
                    "`a`.`b`.`c`",
                    listOf("a", "b", "c").quoteIdentifiers()
                )
            }
        }
    }

    @Test
    fun testDatabase() {
        withMariaDB { testDataSource(it) }
    }
}
