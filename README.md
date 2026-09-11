# KSE

Kotlin SQL Engine

The goal of this project is to provide a lightweight, performant, and easy-to-use JDBC wrapper written in Kotlin.
It is database agnostic and promotes reuse of SQL fragments while giving direct access to JDBC objects.
The library has facilities for marshaling between data projections and objects.

Database agnosticism is achieved by only consuming raw SQL and maintaining no direct knowledge of data types.
Input data are captured in *Param* objects that know how to set their data into prepared statements once given the handle and position.
Output data are read from result sets using JDBC functions directly.
For this last task, there are facilities to conveniently reuse marshaling logic from *ResultSet* to records.

## Example

In this simple example, we start a transaction on the data source, obtaining a *Connection* in context,
and select two fields from a table, returning them as *User* objects.

```kotlin
dataSource.withTransaction {
    select("SELECT id, name FROM users WHERE id = ?", 1.setInt()) {
        readRecords {
            val id = getInt()
            val name = getString()
            User(id, name)
        }
    }
}
```

Note how parameters are bundled.
Extension functions are provided for the data types native to JDBC.
These functions create *Param* objects that, when given a *PreparedStatement* and a position, set the value accordingly.
In this manner, the library needs no special knowledge of supported data types on any database and can easily be 
"taught" new strategies.

The *select* method takes, as its last parameter, a function that consumes a record set.
In this example, we call *readRecords*, which iterates our reader function over the *ResultSet*.
Additionally, it provides a cursor on the row allowing the results to be read in selection order.
The *ResultIterator* derives from *ResultSet*, thus exposing its methods, as well.
Alternatively, one can consume the JDBC native *ResultSet* directly in the *select*.

## Params

Parameters are provided to prepared statements as objects with that operate on the prepared statement.
This scheme obviates the need for reflective code and minimizes the scope of JDBC parameter objects, improving performance.
Extension methods are provided for JDBC types and can be easily added for custom types.

Note in this example of a *Param* implementation that we use the *setNull* function 
to explicitly set SQL NULL when the value is null.
Omitting this can lead to dire consequences.

```kotlin
context(cx: Connection)
fun Collection<String>?.setStrings(): Param =
    setNull { statement, position, value ->
        statement.setArray(position, cx.createArrayOf("TEXT", value.toTypedArray()))
    }
```

## Results

JDBC's *ResultSet* object is available directly to clients, but for convenience, the *ResultIterator* object is provided.
The *ResultIterator* derives from *ResultSet*, thus exposing its methods, as well as providing 
an iterator over the result rows and a cursor on each row, allowing the fields to be read in selected order.

```kotlin
select("SELECT id, name FROM users") {
    readRecords {
        val id = getInt()
        val name = getString()
        User(id, name)
    }
}
```

The *readRecords* function allows clients to simply supply a function to marshal the fields into a record.
The fields may be read in selected order, as here, or using the *ResultSet* methods.

For numerical results, such as inserts and updates, methods are provided to assert the number of rows affected.

## Entities

Entities provide a mapping between fields in a single table and data classes.
By providing a projection and methods for reading and writing a data class, entities simplify the call sites
where these records are read and written.

Each entity is parameterized on two types; one for writing and one for reading.
This handles the situation where a record contains values generated within the database,
such as ids and creation timestamps.
In this case, one can provide a sibling object without the fields or create default values
in the domain object that are ignored when the record is inserted.
In either case, the fields not to be inserted when the row is created must be marked with *auto*.

The *Entity* database methods partially or completely generate some of the SQL.
In the case of *select*, the *Entity* generates the SQL fragment for the projection from the table.
In the case of *insert*, the *Entity* generates the entirety of the SQL.
For *update*, the *Entity* generates the SQL fragment for the partial projection into the table and a WHERE.

```kotlin
val userEntity  = object: Entity<User>(
    table = Table(listOf("KSE"), "USERS"),
    fields = listOf("ID".field.auto, "NAME".field)
) {
    override fun read(resultSet: ResultIterator): User =
        User(resultSet.getInt()!!, resultSet.getString()!!)

    context(cx: Connection)
    override fun write(record: User): List<Param> =
        listOf(record.name.paramString())
}
```

In the above example, the User object is mapped to two fields in the USERS table.
Note the qualifying list of schema names in the table definition.
This may be omitted if the table is in the default schema.

The *auto* tag on the ID field indicates that the field is auto-generated 
and thus to be ignored when inserting the record.
Hence, the *write* method omits it from its parameter list, as well.
Note that all parameters must always be supplied in the same order as the fields in the Entity.

The *read* method is used to map the result set to the User object, 
and the *write* method is used to map the User object to the parameters for insert and update statements.

```kotlin
val users = listOf(User(1, "Alice"), User(2, "Bob"))

userEntity.insert(users).requireInserts(2)
userEntity.update()

val selectedUsers = userEntity.select("U", "WHERE U.ID = ?", 1.paramInt())
```

The example, above, demonstrates the convenience of using an Entity.
Note that the select function requires an alias for the table and an optional tail for the generated SQL.

## Listeners

A listener facility is provided for logging and debugging database activity.
It is provided, optionally, in the *withTransaction* function.
The default does nothing.

The listener methods receive data about various events, namely the SQL statement and their parameters, as applicable.
The *Param* class has an *inspect* method that, by default, renders the value natively as a string (including null).

The LoggingListener implementation is provided with lazy logging methods.
These methods test the log level before committing to avoid unnecessary string formatting.
It is based on *slf4j* because everything else seems to be.

```kotlin
object ExampleListener : LoggingListener(log) {
    override fun select(sql: String, params: List<Param>) {
        info { "SELECT SQL\n$sql${params.showParams()}" }
    }
}

private fun List<Param>.showParams() = withIndex().joinToString("") { "\n\t[${1 + it.index}] ${it.value.inspect()}" }
```

Note the use of the *inspect* method on the parameters in *showParams*.
This allows access to the parameter's value and is intended for logging and debugging.
The default uses the value's *toString* method and can be overridden with a custom *Param* type.

## Aspects

This is a design pattern supported by this library that allows for defining cross-cutting operations on entities that 
have similar forms (field names) but unrelated types.
It is not required that the record types associated with an aspect have any relationship to each other.
The system relies solely on the presence of common fields (by name and type).

Examples of such aspects and operations one might define on them include:
- Entities with ids.
  - Find by id.
- Entities with names.
  - Search by name.
  - Change name.
- Entities with soft delete fields.
  - Change status.

See [Aspects](docs/Aspects.md) for more information and a tutorial.