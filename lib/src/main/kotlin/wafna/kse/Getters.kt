package wafna.kse

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import java.io.InputStream
import java.io.Reader
import java.math.BigDecimal
import java.sql.Array
import java.sql.ResultSet
import java.sql.Timestamp

// Guards NULL for primitive types.
fun <K> ResultSet.orNull(value: K): K? = if (wasNull()) null else value

fun ResultIterator.getString(): String? = getString(next)
fun ResultIterator.getBoolean(): Boolean? = orNull(getBoolean(next))
fun ResultIterator.getByte(): Byte? = orNull(getByte(next))
fun ResultIterator.getShort(): Short? = orNull(getShort(next))
fun ResultIterator.getInt(): Int? = orNull(getInt(next))
fun ResultIterator.getLong(): Long? = orNull(getLong(next))
fun ResultIterator.getFloat(): Float? = orNull(getFloat(next))
fun ResultIterator.getDouble(): Double? = orNull(getDouble(next))
fun ResultIterator.getBigDecimal(): BigDecimal? = getBigDecimal(next)
fun ResultIterator.getBytes(): ByteArray? = getBytes(next)
fun ResultIterator.getDate(): java.sql.Date? = getDate(next)
fun ResultIterator.getTime(): java.sql.Time? = getTime(next)
fun ResultIterator.getTimestamp(): Timestamp? = getTimestamp(next)
fun ResultIterator.getAsciiStream(): InputStream? = getAsciiStream(next)
fun ResultIterator.getCharacterStream(): Reader? = getCharacterStream(next)
fun ResultIterator.getBinaryStream(): InputStream? = getBinaryStream(next)


fun ResultIterator.getObject(): Any? = getObject(next)

fun ResultIterator.getArray(): Array? = getArray(next)

// NB the PGSQL type DATE is equivalent to Kotlin's LocalDate
fun ResultIterator.getLocalDate(): LocalDate? =
    getDate(next)?.toLocalDate()?.let { ld ->
        LocalDate(ld.year, Month(ld.month.value), ld.dayOfMonth)
    }

