package wafna.kse

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import java.sql.Array
import java.sql.ResultSet
import java.sql.SQLType
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
fun Int.requireUpdates(count: Int) = require(this == count) {
    "Expected $count updates, actual $this."
}

/**
 * Assert the number of records affected by an INSERT.
 */
fun IntArray.requireInserts(count: Int) = sum().let {
    require(it == count) {
        "Expected $count inserts, actual $it."
    }
}

/**
 * Convenience class for reading the fields from a ResultSet in fixed order, e.g. the declared field
 * order in an Entity. This also makes the nullability of data from the ResultSet more explicit.
 */
class ResultSetFieldIterator(val rs: ResultSet) : ResultSet by rs {
    private var position = 0
    val next: Int
        get() = ++position

    override fun updateObject(
        columnIndex: Int,
        x: Any?,
        targetSqlType: SQLType?,
        scaleOrLength: Int
    ) {
        rs.updateObject(columnIndex, x, targetSqlType, scaleOrLength)
    }

    override fun updateObject(
        columnLabel: String?,
        x: Any?,
        targetSqlType: SQLType?,
        scaleOrLength: Int
    ) {
        rs.updateObject(columnLabel, x, targetSqlType, scaleOrLength)
    }

    override fun updateObject(columnIndex: Int, x: Any?, targetSqlType: SQLType?) {
        rs.updateObject(columnIndex, x, targetSqlType)
    }

    override fun updateObject(columnLabel: String?, x: Any?, targetSqlType: SQLType?) {
        rs.updateObject(columnLabel, x, targetSqlType)
    }
}

@Suppress("RedundantNullableReturnType")
fun ResultSetFieldIterator.getBoolean(): Boolean? = getBoolean(next)

@Suppress("RedundantNullableReturnType")
fun ResultSetFieldIterator.getInt(): Int? = getInt(next)

// TODO need to check wasNull everywhere!
@Suppress("RedundantNullableReturnType")
fun ResultSetFieldIterator.getDouble(): Double? = getDouble(next)

fun ResultSetFieldIterator.getString(): String? = getString(next)

fun ResultSetFieldIterator.getObject(): Any? = getObject(next)

fun ResultSetFieldIterator.getTimestamp(): Timestamp? = getTimestamp(next)

fun ResultSetFieldIterator.getArray(): Array? = getArray(next)

// NB the PGSQL type DATE is equivalent to Kotlin's LocalDate
fun ResultSetFieldIterator.getLocalDate(): LocalDate? =
    getDate(next)?.toLocalDate()?.let { ld ->
        LocalDate(ld.year, Month(ld.month.value), ld.dayOfMonth)
    }

/** Read a single record from a result set using a field iterator function. */
inline fun <R> ResultSet.readRecord(run: ResultSetFieldIterator.() -> R): R = ResultSetFieldIterator(this).run()

/** Read all the records from a result set using a field iterator function. */
inline fun <R> ResultSet.readRecords(run: ResultSetFieldIterator.() -> R): List<R> =
    buildList {
        while (next()) add(readRecord(run))
    }

/** This is used to produce param lists from source records on demand. */
fun <P, Q> Iterable<P>.transformer(f: (P) -> Q): Iterator<Q> {
    val it = iterator()
    return object : Iterator<Q> {
        override fun next(): Q = f(it.next())

        override fun hasNext(): Boolean = it.hasNext()
    }
}
