package wafna.kse.aspects

import wafna.kse.Entity
import wafna.kse.Listener
import wafna.kse.setArray
import wafna.kse.setString
import java.sql.Connection

internal interface Selectable<R> {
    context(_: Connection, _: Listener)
    suspend fun selectByIds(ids: Collection<Int>): List<R>
    context(_: Connection, _: Listener)
    suspend fun searchByName(searchTarget: String): List<R>
}

private class SelectableImpl<R>(val entity: Entity<R, *>) : Selectable<R> {
    context(_: Connection, _: Listener)
    override suspend fun selectByIds(ids: Collection<Int>): List<R> =
        if (ids.isEmpty()) emptyList()
        else entity.select("", "WHERE id = ANY(?::INT[])", ids.setArray("INT"))

    context(_: Connection, _: Listener)
    override suspend fun searchByName(searchTarget: String): List<R> =
        if (searchTarget.isBlank()) emptyList()
        else entity.select("", "WHERE name LIKE ?", searchTarget.setString())
}

internal fun <R> selectable(entity: Entity<R, *>): Selectable<R> = SelectableImpl(entity)

