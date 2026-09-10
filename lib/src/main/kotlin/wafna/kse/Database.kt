package wafna.kse

import javax.sql.DataSource
import org.slf4j.Logger
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

class DBException(msg: String, cause: Throwable) : RuntimeException(msg, cause)

/**
 * Execute the given block within a transaction.
 * The transaction is committed if the block completes normally and rolled back if it throws an exception.
 */
suspend fun <T> DataSource.withTransaction(
    listener: Listener = ListenerNOOP,
    borrow: suspend context(Connection, Listener) () -> T
): T =
    connection.use { connection ->
        connection.autoCommit = false
        connection.beginRequest()
        // Used to avoid masking exceptions.
        var success = false
        try {
            context(connection, listener) { borrow() }.also {
                connection.commit()
                success = true
            }
        } catch (e: Throwable) {
            try {
                connection.rollback()
            } catch (e: Throwable) {
                // We don't want to mask the original exception.
                e.printStackTrace()
            }
            throw e
        } finally {
            try {
                connection.endRequest()
            } catch (e: Throwable) {
                // We don't want to mask the original exception.
                if (success)
                    throw e
                e.printStackTrace()
            }
        }
    }

/**
 * Interpolates the params into the prepared statement in order.
 */
private fun PreparedStatement.setParams(params: Iterable<Param>) =
    params.forEachIndexed { index, param -> param.set(this, 1 + index) }

/**
 * Interpolates the params into the prepared statement in order.
 */
private fun PreparedStatement.setParams(params: Array<out Param>) =
    params.forEachIndexed { index, param -> param.set(this, 1 + index) }

context(cx: Connection, listener: Listener)
private suspend inline fun <T> withStatement(
    sql: String,
    borrow: suspend PreparedStatement.() -> T,
): T {
    listener.execute(sql)
    return runCatching { cx.prepareStatement(sql).use { it.borrow() } }
        .getOrElse {
            throw DBException("Error while executing SQL\n$sql", it)
        }
}

/**
 * PreparedStatment.executeQuery()
 */
context(_: Connection, listener: Listener)
suspend fun <T> select(
    sql: String,
    vararg params: Param,
    reader: ResultSet.() -> T,
): T {
    listener.select(sql, params.toList())
    return withStatement(sql) {
        setParams(params)
        executeQuery().use { it.reader() }
    }
}

/**
 * PreparedStatment.executeBatch()
 * To simplify usage and reduce memory footprint, the records are presented as an Iterator.
 * Callers should produce the param lists for each record on demand.
 */
context(_: Connection, listener: Listener)
suspend fun insert(
    sql: String,
    records: Iterator<List<Param>>,
): IntArray {
    listener.insert(sql)
    return withStatement(sql) {
        records.forEach { record ->
            listener.insertRecord(record)
            setParams(record)
            addBatch()
        }
        executeBatch()
    }
}

/**
 * PreparedStatment.executeUpdate()
 */
context(_: Connection, listener: Listener)
suspend fun update(
    sql: String,
    vararg params: Param,
): Int {
    listener.update(sql, params.toList())
    return withStatement(sql) {
        setParams(params)
        executeUpdate()
    }
}

/**
 * PreparedStatment.executeUpdate()
 */
context(_: Connection, listener: Listener)
suspend fun update(
    sql: String,
    params: Collection<Param>,
): Int {
    listener.update(sql, params.toList())
    return withStatement(sql) {
        setParams(params)
        executeUpdate()
    }
}

/**
 * Surrounds an identifier in quotes.
 */
context(cx: Connection)
fun String.quoteIdentifier(): String =
    cx.metaData.identifierQuoteString.let { "$it$this$it" }

/**
 * Produces a dot-separated list of quoted identifiers, i.e. for qualified names.
 */
context(_: Connection)
fun Iterable<String>.quoteIdentifiers(): String =
    joinToString(".") { it.quoteIdentifier() }
