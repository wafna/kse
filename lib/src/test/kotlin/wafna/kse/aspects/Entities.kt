package wafna.kse.aspects

import wafna.kse.Entity
import wafna.kse.Param
import wafna.kse.ResultIterator
import wafna.kse.Table
import wafna.kse.auto
import wafna.kse.field
import wafna.kse.setInt
import wafna.kse.setString
import java.sql.Connection

internal object UserEntity : Entity<User, UserWip>(
    Table("users"),
    listOf("id".field.auto, "name".field)
) {
    override fun read(): ResultIterator.() -> User = {
        User(id = getInt()!!, name = getString()!!)
    }

    context(cx: Connection)
    override fun write(): UserWip.() -> List<Param> = {
        listOf(name.setString())
    }
}

internal object DocumentEntity : Entity<Document, DocumentWip>(
    table = Table("documents"),
    fields = listOf("id".field.auto, "name".field, "owner_id".field)
) {
    override fun read(): ResultIterator.() -> Document = {
        Document(getInt()!!, getString()!!, getInt()!!)
    }

    context(cx: Connection)
    override fun write(): DocumentWip.() -> List<Param> = {
        listOf(name.setString(), ownerId.setInt())
    }
}