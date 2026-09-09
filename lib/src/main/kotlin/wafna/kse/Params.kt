package wafna.kse

import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.sql.Types
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

typealias Param = (statement: PreparedStatement, parameterIndex: Int) -> Unit

val paramNull: Param = { statement, parameterIndex -> statement.setNull(parameterIndex, Types.NULL) }

/** Explicitly sets NULL for null parameters. */
fun <T> T?.setNullable(setter: (PreparedStatement, Int, T) -> Unit): Param =
    if (null == this) paramNull
    else { statement, parameterIndex -> setter(statement, parameterIndex, this) }

fun Int?.setInt(): Param = setNullable { statement, parameterIndex, value ->
    statement.setInt(parameterIndex, value)
}

fun Double?.setDouble(): Param = setNullable { statement, parameterIndex, value ->
    statement.setDouble(parameterIndex, value)
}

fun String?.setString(): Param = setNullable { statement, parameterIndex, value ->
    statement.setString(parameterIndex, value)
}

fun Instant?.setInstant(): Param = setNullable { statement, parameterIndex, value ->
    val dateTime =
        LocalDateTime.ofInstant(
            value.toJavaInstant(),
            ZoneId.of(Calendar.getInstance().timeZone.id),
        )
    statement.setTimestamp(parameterIndex, Timestamp.valueOf(dateTime))
}

fun Boolean?.setBoolean(): Param = setNullable { statement, parameterIndex, value ->
    statement.setBoolean(parameterIndex, value)
}

fun LocalDate?.setLocalDate(): Param = setNullable { statement, parameterIndex, value ->
    statement.setDate(parameterIndex, java.sql.Date.valueOf(value.toJavaLocalDate()))
}

fun Any?.setObject(): Param = setNullable { statement, parameterIndex, value ->
    statement.setObject(parameterIndex, value)
}

context(cx: Connection)
inline fun <reified K> Collection<K>.setArray(type: String): Param =
    setNullable { statement, position, value ->
        statement.setArray(position, cx.createArrayOf(type, value.toTypedArray<K>()))
    }

