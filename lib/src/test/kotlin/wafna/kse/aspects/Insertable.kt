package wafna.kse.aspects

import wafna.kse.Entity
import wafna.kse.Listener
import java.sql.Connection

internal interface Insertable<W> {
    context(_: Connection, _: Listener)
    suspend fun insert(items: Iterable<W>): IntArray
}

private class InsertableImpl<W>(val entity: Entity<*, W>) : Insertable<W> {
    context(_: Connection, _: Listener)
    override suspend fun insert(items: Iterable<W>): IntArray = entity.insert(items)
}

internal fun <W> insertable(entity: Entity<*, W>): Insertable<W> = InsertableImpl(entity)