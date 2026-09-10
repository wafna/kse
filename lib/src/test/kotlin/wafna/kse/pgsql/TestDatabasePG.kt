package wafna.kse.pgsql

import kotlin.test.Test
import kotlin.test.assertEquals
import wafna.kse.common.testDataSource
import wafna.kse.h2.withH2DB
import wafna.kse.quoteIdentifiers
import wafna.kse.withTransaction

class TestDatabasePG {
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
        withPGDB { testDataSource(it) }
    }
}