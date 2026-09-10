package wafna.kse

import java.io.InputStream
import java.io.Reader
import java.math.BigDecimal
import java.sql.Connection
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.Time
import java.sql.Timestamp
import java.sql.Types
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate

typealias Param = (statement: PreparedStatement, parameterIndex: Int) -> Unit

val setNull: Param = { statement, parameterIndex -> statement.setNull(parameterIndex, Types.NULL) }

/** Explicitly sets NULL for null parameters. */
fun <T> T?.setNullable(setter: (PreparedStatement, Int, T) -> Unit): Param =
    if (null == this) setNull
    else { statement, parameterIndex -> setter(statement, parameterIndex, this) }

fun String?.setString(): Param = setNullable { statement, parameterIndex, value ->
    statement.setString(parameterIndex, value)
}

fun Boolean?.setBoolean(): Param = setNullable { statement, parameterIndex, value ->
    statement.setBoolean(parameterIndex, value)
}

fun Byte?.setByte(): Param = setNullable { statement, parameterIndex, value ->
    statement.setByte(parameterIndex, value)
}

fun Short?.setShort(): Param = setNullable { statement, parameterIndex, value ->
    statement.setShort(parameterIndex, value)
}

fun Int?.setInt(): Param = setNullable { statement, parameterIndex, value ->
    statement.setInt(parameterIndex, value)
}

fun Long?.setLong(): Param = setNullable { statement, parameterIndex, value ->
    statement.setLong(parameterIndex, value)
}

fun Float?.setFloat(): Param = setNullable { statement, parameterIndex, value ->
    statement.setFloat(parameterIndex, value)
}

fun Double?.setDouble(): Param = setNullable { statement, parameterIndex, value ->
    statement.setDouble(parameterIndex, value)
}

fun BigDecimal?.setBigDecimal(): Param = setNullable { statement, parameterIndex, value ->
    statement.setBigDecimal(parameterIndex, value)
}

fun ByteArray?.setBytes(): Param = setNullable { statement, parameterIndex, value ->
    statement.setBytes(parameterIndex, value)
}

fun Date?.setDate(): Param = setNullable { statement, parameterIndex, value ->
    statement.setDate(parameterIndex, value)
}

fun Time?.setTime(): Param = setNullable { statement, parameterIndex, value ->
    statement.setTime(parameterIndex, value)
}

fun Timestamp?.setTimestamp(): Param = setNullable { statement, parameterIndex, value ->
    statement.setTimestamp(parameterIndex, value)
}

fun InputStream?.setAsciiStream(): Param = setNullable { statement, parameterIndex, value ->
    statement.setAsciiStream(parameterIndex, value)
}

fun Reader?.setCharacterStream(): Param = setNullable { statement, parameterIndex, value ->
    statement.setCharacterStream(parameterIndex, value)
}

fun InputStream?.setBinaryStream(): Param = setNullable { statement, parameterIndex, value ->
    statement.setBinaryStream(parameterIndex, value)
}

fun Instant?.setInstant(): Param = setNullable { statement, parameterIndex, value ->
    val dateTime =
        LocalDateTime.ofInstant(
            value.toJavaInstant(),
            ZoneId.of(Calendar.getInstance().timeZone.id),
        )
    statement.setTimestamp(parameterIndex, Timestamp.valueOf(dateTime))
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

