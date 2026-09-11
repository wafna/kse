# Aspects

Aspects are a design pattern supported by *Entity* that allows for SQL patterns to be applied to
multiple unrelated entities that have similar fields.
The goal is to simplify call sites on the database.

Let's have a look.

```kotlin
interface Selectable<T> {
    context(_: Connection)
    suspend fun selectByIds(ids: Collection<Int>): List<T>
}
```

Above, we're specifying an action that selects records en masse from a list of ids.
We'll implement this, directly.

```kotlin
private class SelectableImpl(val entity: Entity<T>) : SelectableByExternalId<E> {
    context(_: Connection)
    override suspend fun selectByIds(ids: Collection<Int>): List<T> =
        if (ids.isEmpty()) {
            emptyList()
        } else {
            entity.select(
                "",
                "WHERE id = ANY(?::INT[])",
                ids.paramStrings(), // Defined, above.
            )
        }
}

fun <T> selectableById(entity: Entity<T>): Selectable<E> = SelectableImpl(entity)
```

We rely on the *Entity* to supply the projection and read the fields.
Here, we only require the presence of an integer *id* field.

```kotlin
// The particular entity to be selected by id.
object UserEntity : Entity<User>(
    Table("users"),
    listOf("id".field, "name".field)
) {
    override fun read(resultSet: ResultSetFieldIterator): User = with(resultSet) {
        User(id = getInt()!!, name = getString()!!)
    }

    context(cx: Connection)
    override fun write(record: User): List<Param> = record.run {
        listOf(id.paramAny, name.paramString)
    }
}
```

Above is an example *Entity* for a user object, which we'll use, below.

```kotlin
// Mixed into an object dedicated to working with User records.
class UserDao : Selectable<User> by selectableById(Entity<User>) {
    // ...
}
```

Using delegated inheritance, all the methods of *Selectable*, implemented on *Users*, are now available in *UserDao*.

Multiple instances of these aspects can be mixed in.
A more complete example is worked out in the test code.

```kotlin

```