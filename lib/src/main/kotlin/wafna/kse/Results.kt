package wafna.kse

import java.sql.ResultSet
import java.sql.SQLType

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
class ResultIterator(val rs: ResultSet) : ResultSet by rs {
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

/** Read a single record from a result set using a field iterator function. */
inline fun <R> ResultSet.readRecord(run: ResultIterator.() -> R): R = ResultIterator(this).run()

/** Read all the records from a result set using a field iterator function. */
inline fun <R> ResultSet.readRecords(run: ResultIterator.() -> R): List<R> =
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
