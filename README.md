# KSE

Kotlin SQL Engine

The goal of this project is to provide a lightweight, performant, and easy-to-use JDBC wrapper written in Kotlin.
It is database agnostic and promotes reuse of SQL queries while giving direct access to JDBC objects.
The library is optimized for marshaling between data projections and objects.

Database agnosticism is achieved by only consuming raw SQL and maintaining no direct knowledge of data types.
Input data are encoded as functions that know how to set their data into prepared statements once given the handle and position.
Output data are read from result sets using JDBC functions directly.

## Example

In this simple example, we start a transaction on the data source and select two fields from a table.

```kotlin
dataSource.withTransaction {
    select("SELECT id, name FROM test WHERE id = ?", 1.setInt()) {
        readRecords {
            val id = getInt()
            val name = getString()
            id to name
        }
    }
}
```

Note how parameters are bundled.
Extension functions are provided for the data types native to JDBC.
These functions create functions that, when given a *PreparedStatement* and a position, set the value accordingly.
In this manner, the library assumes nothing about supported data types and custom types can easily be supported 
in client code.

The *select* method takes, as its last parameter, a function that consumes a record set.
In this example, we call readRecords, which iterates our reader function over the *ResultSet*.
Additionally, it maintains a cursor on the row allowing the results to be read in order.
The *ResultIterator* derives from *ResultSet*, thus exposing its methods, as well.

## Params

Parameters are provided to prepared statements as setter functions that operate on the prepared statement.
This scheme obviates the need for reflective code, improving performance, and allows for easy extension.
Extension methods are provided for JDBC types and can be easily added for custom types.

Note in this example that we use the *setNull* function to explicitly set SQL NULL when the value is null.

```kotlin
fun String?.paramString(): Param = setNull { statement, parameterIndex, value ->
    statement.setString(parameterIndex, value)
}
```

## Results

JDBC's *ResultSet* object is available directly to clients but, for convenience, the *ResultIterator* object is provided.
The *ResultIterator* derives from *ResultSet*, thus exposing its methods, as well as providing 
an iterator over the result rows and a cursor on each row, allowing the fields to be read in order.

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

For numerical results, i.e. inserts and updates, methods are provided to assert the number of rows affected.

## Entities

Entities provide a mapping between fields in a single table and a data class.
By providing a projection and methods for reading and writing a data class, entities simplify the call sites
where these records are read and written.

```kotlin
val userEntity  = object: Entity<User>(
    table = Table(listOf("KSE"), "USERS"),
    fields = listOf("ID".field, "NAME".field)
) {
    override fun read(resultSet: ResultIterator): User =
        User(resultSet.getInt()!!, resultSet.getString()!!)

    context(cx: Connection)
    override fun write(record: User): List<Param> =
        listOf(record.id.paramInt(), record.name.paramString())
}
```

In the above example, the User object is mapped to two fields in the USERS table.
Note the qualifying list of schema names in the table definition.
This may be omitted if the table is in the default schema.

The *read* method is used to map the result set to the User object, 
and the *write* method is used to map the User object to the parameters for insert and update statements.

```kotlin
val users = listOf(User(1, "Alice"), User(2, "Bob"))

userEntity.insert(users).requireInserts(2)

val selectedUsers = userEntity.select("U", "WHERE U.ID = ?", 1.paramInt())
```

The example, above, demonstrates the convenience of using an Entity.
Note that the select function requires an alias for the table and an optional tail for the generated SQL.