package wafna.kse.h2

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import wafna.kse.orNull
import wafna.kse.readRecords
import wafna.kse.requireUpdates
import wafna.kse.select
import wafna.kse.setArray
import wafna.kse.setAsciiStream
import wafna.kse.setBigDecimal
import wafna.kse.setBinaryStream
import wafna.kse.setBoolean
import wafna.kse.setByte
import wafna.kse.setBytes
import wafna.kse.setCharacterStream
import wafna.kse.setDate
import wafna.kse.setDouble
import wafna.kse.setFloat
import wafna.kse.setInt
import wafna.kse.setLocalDate
import wafna.kse.setLong
import wafna.kse.setObject
import wafna.kse.setShort
import wafna.kse.setString
import wafna.kse.setTime
import wafna.kse.setTimestamp
import wafna.kse.update
import wafna.kse.withTransaction
import java.math.BigDecimal
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp

class TestJDBCTypesH2 {
    @Test
    fun testAllNonNull() {
        withH2DB { db ->
            db.withTransaction {
                update(
                    """
                    CREATE TABLE IF NOT EXISTS test_getters_all (
                        id INT PRIMARY KEY,
                        c_string VARCHAR(255),
                        c_boolean BOOLEAN,
                        c_byte TINYINT,
                        c_short SMALLINT,
                        c_int INT,
                        c_long BIGINT,
                        c_float REAL,
                        c_double DOUBLE PRECISION,
                        c_bigdecimal DECIMAL(10, 2),
                        c_bytes VARBINARY(255),
                        c_date DATE,
                        c_time TIME,
                        c_timestamp TIMESTAMP,
                        c_ascii CLOB,
                        c_char CLOB,
                        c_binary BLOB,
                        c_object VARCHAR(255),
                        c_array INT ARRAY,
                        c_localdate DATE
                    )
                    """.trimIndent()
                )

                val expectedDate = Date.valueOf("2026-09-09")
                val expectedTime = Time.valueOf("14:30:00")
                val expectedTimestamp = Timestamp.valueOf("2026-09-09 14:30:00.123")
                val expectedBytes = byteArrayOf(1, 2, 3, 4, 5)
                val expectedBigDecimal = BigDecimal("123.45")
                val expectedLocalDate = LocalDate(2026, Month.SEPTEMBER, 9)
                val expectedArray = listOf(10, 20, 30)

                update(
                    """
                    INSERT INTO test_getters_all (
                        id, c_string, c_boolean, c_byte, c_short, c_int, c_long, c_float, c_double,
                        c_bigdecimal, c_bytes, c_date, c_time, c_timestamp, c_ascii, c_char,
                        c_binary, c_object, c_array, c_localdate
                    ) VALUES (
                        ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                    )
                    """.trimIndent(),
                    1.setInt(),
                    "sample_string".setString(),
                    true.setBoolean(),
                    12.toByte().setByte(),
                    1234.toShort().setShort(),
                    123456.setInt(),
                    1234567890123L.setLong(),
                    3.14f.setFloat(),
                    2.718281828.setDouble(),
                    expectedBigDecimal.setBigDecimal(),
                    expectedBytes.setBytes(),
                    expectedDate.setDate(),
                    expectedTime.setTime(),
                    expectedTimestamp.setTimestamp(),
                    "ascii_text".byteInputStream().setAsciiStream(),
                    "char_text".reader().setCharacterStream(),
                    expectedBytes.inputStream().setBinaryStream(),
                    "custom_object".setObject(),
                    expectedArray.setArray("INTEGER"),
                    expectedLocalDate.setLocalDate()
                ).requireUpdates(1)

                select("SELECT c_string, c_boolean, c_byte, c_short, c_int, c_long, c_float, c_double, c_bigdecimal, c_bytes, c_date, c_time, c_timestamp, c_ascii, c_char, c_binary, c_object, c_array, c_localdate FROM test_getters_all WHERE id = 1") {
                    readRecords {
                        assertEquals("sample_string", getString())
                        assertEquals(true, getBoolean())
                        assertEquals(12.toByte(), getByte())
                        assertEquals(1234.toShort(), getShort())
                        assertEquals(123456, getInt())
                        assertEquals(1234567890123L, getLong())
                        assertEquals(3.14f, getFloat())
                        assertEquals(2.718281828, getDouble())
                        assertEquals(expectedBigDecimal, getBigDecimal())
                        assertContentEquals(expectedBytes, getBytes())
                        assertEquals(expectedDate, getDate())
                        assertEquals(expectedTime, getTime())
                        assertEquals(expectedTimestamp, getTimestamp())
                        assertEquals("ascii_text", getAsciiStream()?.bufferedReader()?.readText())
                        assertEquals("char_text", getCharacterStream()?.readText())
                        assertContentEquals(expectedBytes, getBinaryStream()?.readAllBytes())
                        assertEquals("custom_object", getObject())
                        val arrayResult = (getArray()?.array as? Array<*>)?.toList()
                        assertEquals(expectedArray, arrayResult)
                        assertEquals(expectedLocalDate, getLocalDate())
                    }
                }.also {
                    assertEquals(1, it.size)
                }
            }
        }
    }

    @Test
    fun testAllNull() {
        withH2DB { db ->
            db.withTransaction {
                update(
                    """
                    CREATE TABLE IF NOT EXISTS test_getters_null (
                        id INT PRIMARY KEY,
                        c_string VARCHAR(255),
                        c_boolean BOOLEAN,
                        c_byte TINYINT,
                        c_short SMALLINT,
                        c_int INT,
                        c_long BIGINT,
                        c_float REAL,
                        c_double DOUBLE PRECISION,
                        c_bigdecimal DECIMAL(10, 2),
                        c_bytes VARBINARY(255),
                        c_date DATE,
                        c_time TIME,
                        c_timestamp TIMESTAMP,
                        c_ascii CLOB,
                        c_char CLOB,
                        c_binary BLOB,
                        c_object VARCHAR(255),
                        c_array INT ARRAY,
                        c_localdate DATE
                    )
                    """.trimIndent()
                )

                update(
                    """
                    INSERT INTO test_getters_null (
                        id, c_string, c_boolean, c_byte, c_short, c_int, c_long, c_float, c_double,
                        c_bigdecimal, c_bytes, c_date, c_time, c_timestamp, c_ascii, c_char,
                        c_binary, c_object, c_array, c_localdate
                    ) VALUES (
                        2, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
                        NULL, NULL, NULL, NULL, NULL, NULL, NULL,
                        NULL, NULL, NULL, NULL
                    )
                    """.trimIndent()
                ).requireUpdates(1)

                select("SELECT c_string, c_boolean, c_byte, c_short, c_int, c_long, c_float, c_double, c_bigdecimal, c_bytes, c_date, c_time, c_timestamp, c_ascii, c_char, c_binary, c_object, c_array, c_localdate FROM test_getters_null WHERE id = 2") {
                    readRecords {
                        assertNull(getString())
                        assertNull(getBoolean())
                        assertNull(getByte())
                        assertNull(getShort())
                        assertNull(getInt())
                        assertNull(getLong())
                        assertNull(getFloat())
                        assertNull(getDouble())
                        assertNull(getBigDecimal())
                        assertNull(getBytes())
                        assertNull(getDate())
                        assertNull(getTime())
                        assertNull(getTimestamp())
                        assertNull(getAsciiStream())
                        assertNull(getCharacterStream())
                        assertNull(getBinaryStream())
                        assertNull(getObject())
                        assertNull(getArray())
                        assertNull(getLocalDate())
                    }
                }.also {
                    assertEquals(1, it.size)
                }
            }
        }
    }

    @Test
    fun testOrNull() {
        withH2DB { db ->
            db.withTransaction {
                update("CREATE TABLE IF NOT EXISTS test_ornull (id INT PRIMARY KEY, val INT)")
                update("INSERT INTO test_ornull VALUES (1, NULL)")
                select("SELECT val FROM test_ornull WHERE id = 1") {
                    while (next()) {
                        val v = getInt(1)
                        assertNull(orNull(v))
                    }
                }
            }
        }
    }
}
