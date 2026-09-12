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
This interface is required to support multiple mix-ins via delegation.
We'll implement this, directly.

```kotlin
class SelectableImpl(val entity: Entity<T>) : Selectable<E> {
    context(_: Connection)
    override suspend fun selectByIds(ids: Collection<Int>): List<T> =
        if (ids.isEmpty()) {
            emptyList()
        } else {
            entity.select("", "WHERE id = ANY(?::INT[])", ids.setArray("INT"))
        }
}

```

We rely on the entity to supply the projection and marshal the data.
Here, we only require the presence of an integer *id* field.

```kotlin
class UserDao : Selectable<User> by SelectableImpl(UserEntity) {
    // ...
}
```

Using delegated inheritance, all the methods of *Selectable*, implemented on *Users*, are now available in *UserDao*.

Multiple instances of these aspects can be mixed in.

A more complete example is worked out in the [test code](../lib/src/test/kotlin/wafna/kse/aspects/TestAspect.kt).
