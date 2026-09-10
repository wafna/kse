package wafna.kse.h2

import kotlin.test.Test
import kotlin.test.assertEquals
import wafna.kse.common.testDataSource
import wafna.kse.quoteIdentifiers
import wafna.kse.withTransaction

class TestDatabaseH2 {
    @Test
    fun testQuoteIdentifier() {
        withH2DB { db ->
            db.withTransaction {
                assertEquals(
                    """"a"."b"."c"""",
                    listOf("a", "b", "c").quoteIdentifiers()
                )
            }
        }
    }
    @Test
    fun testDatabase() {
        withH2DB { testDataSource(it) }
    }
}