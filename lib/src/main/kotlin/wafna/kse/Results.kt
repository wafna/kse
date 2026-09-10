package wafna.kse

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import java.io.InputStream
import java.io.Reader
import java.math.BigDecimal
import java.sql.Array
import java.sql.ResultSet
import java.sql.Timestamp

/** Enforces that the result of a SELECT contains no more that one record. */
val <T> List<T>.optional: T?
    get() =
        when (size) {
            0 -> null
            1 -> first()
            else -> throw IllegalStateException("Multiple results received.")
        }

val <T> List<T>.unique: T
    get() = optional ?: throw IllegalStateException("No results received for $this")

/**
 * Assert the number of records affected by an UPDATE.
 */
fun Int.requireUpdates(count: Int) =
    require(this == count) { "Expected $count updates, actual $this." }

/**
 * Assert the number of records affected by an INSERT.
 */
fun IntArray.requireInserts(count: Int) = sum().let {
    require(it == count) { "Expected $count inserts, actual $it." }
}

/**
 * Convenience class for reading the fields from a ResultSet in fixed order, e.g. the declared field
 * order in an Entity. This also makes the nullability of data from the ResultSet more explicit.
 */
@Suppress("JavaDefaultMethodsNotOverriddenByDelegation")
class ResultIterator(val rs: ResultSet) : ResultSet by rs {
    private var position = 0
    val next: Int
        get() = ++position

    fun getString(): String? = getString(next)
    fun getBoolean(): Boolean? = orNull(getBoolean(next))
    fun getByte(): Byte? = orNull(getByte(next))
    fun getShort(): Short? = orNull(getShort(next))
    fun getInt(): Int? = orNull(getInt(next))
    fun getLong(): Long? = orNull(getLong(next))
    fun getFloat(): Float? = orNull(getFloat(next))
    fun getDouble(): Double? = orNull(getDouble(next))
    fun getBigDecimal(): BigDecimal? = getBigDecimal(next)
    fun getBytes(): ByteArray? = getBytes(next)
    fun getDate(): java.sql.Date? = getDate(next)
    fun getTime(): java.sql.Time? = getTime(next)
    fun getTimestamp(): Timestamp? = getTimestamp(next)
    fun getAsciiStream(): InputStream? = getAsciiStream(next)
    fun getCharacterStream(): Reader? = getCharacterStream(next)
    fun getBinaryStream(): InputStream? = getBinaryStream(next)
    fun getObject(): Any? = getObject(next)
    fun getArray(): Array? = getArray(next)
    fun getLocalDate(): LocalDate? =
        getDate(next)?.toLocalDate()?.let { ld ->
            LocalDate(ld.year, Month(ld.month.value), ld.dayOfMonth)
        }
}

// Guards NULL for primitive types.
fun <K> ResultSet.orNull(value: K): K? = if (wasNull()) null else value

/** Read a single record from a result set using a field iterator function. */
inline fun <R> ResultSet.readRecord(run: ResultIterator.() -> R): R = ResultIterator(this).run()

/** Read all the records from a result set using a field iterator function. */
inline fun <R> ResultSet.readRecords(run: ResultIterator.() -> R): List<R> =
    buildList {
        while (next()) add(readRecord(run))
    }

