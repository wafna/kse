package wafna.kse

import java.sql.Connection

/** An element of the projection that maps to an object These are used to generate SQL. */
data class Field(val name: String, val sqlType: String? = null) {
    init {
        require(name.isNotEmpty()) { "Field name required." }
        require(sqlType?.isEmpty() != true) { "sqlType must be non-empty if provided." }
    }
}

/** Syntactic convenience. */
fun String.field(sqlType: String? = null) = Field(this, sqlType)

/** Syntactic convenience. */
val String.field: Field
    get() = Field(this, null)

/**
 * The fully qualified table name.
 */
data class Table(val schemas: List<String>, val tableName: String) {
    init {
        require(schemas.all { it.isNotEmpty() }) {
            "Empty schema names prohibited: ${schemas.joinToString(", ") { "\"$it\"" }}"
        }
        require(tableName.isNotEmpty()) { "Empty table name prohibited." }
    }

    constructor(tableName: String) : this(emptyList(), tableName)

    context(_: Connection)
    fun qname() = (schemas + tableName).quoteIdentifiers()

    override fun toString(): String = throw NotImplementedError("Do not use.")
}

/**
 * A mapping between fields in a single table and a data class.
 */
abstract class Entity<R>(val table: Table, val fields: List<Field>) {
    init {
        val duplicates = fields.groupBy { it.name }.filter { 1 < it.value.size }
        require(duplicates.isEmpty()) {
            "Duplicate fields detected: ${duplicates.keys.joinToString(", ")}."
        }
    }

    fun projection(alias: String): String = fields.joinToString(", ") {
        if (alias.isEmpty()) "\"${it.name}\"" else "\"$alias\".\"${it.name}\""
    }

    val fieldMap = fields.associateBy { it.name }

    abstract fun read(resultSet: ResultIterator): R

    /**
     * Create a list of parameter setters in the order of the fields from a record. Some types require
     * the connection to instantiate, e.g. java.sql.Array.
     */
    context(cx: Connection)
    abstract fun write(record: R): List<Param>

    /** Generates the head of a SELECT statement. */
    context(_: Connection)
    fun selectHead(alias: String = ""): String =
        "SELECT ${projection(alias)}\nFROM ${table.qname()}${if (alias.isEmpty()) " " else " \"$alias\""}"

    /**
     * @param alias The alias applied to the table in the head.
     * @param tail The SQL following the head, e.g. JOIN and WHERE.
     * @param params The values of the arguments in the SQL in lexical order.
     */
    context(_: Connection, _: Listener)
    suspend fun select(
        alias: String,
        tail: String,
        vararg params: Param,
    ): List<R> = select("${selectHead(alias)}\n$tail", *params) { readRecords(::read) }

    context(c_: Connection, _: Listener)
    suspend fun select(
        alias: String,
        tail: String,
        params: Collection<Param>,
    ): List<R> =
        select("${selectHead(alias)}\n$tail", *params.toTypedArray()) {
            readRecords(::read)
        }

    /**
     * Every INSERT has the same form. This variable form is useful for tables with default fields
     * that are not inserted (e.g. deleted_at). This enforces that the fields exist.
     */
    context(_: Connection)
    private fun insertHead(fieldNames: Iterable<String>): String =
        """INSERT INTO ${table.qname()} (${fieldNames.joinToString(", ") { it.quoteIdentifier() }})
            VALUES (${parameterList(namesToFields(fieldNames))})""".trimIndent()

    context(_: Connection, _: Listener)
    suspend fun insert(
        records: Iterable<R>,
    ): IntArray = insert(
        sql = insertHead(fields.map { it.name }),
        records = records.transformer { write(it) },
    )

    context(_: Connection, _: Listener)
    suspend fun update(
        fieldNames: Iterable<String>,
        where: String,
        vararg params: Param,
    ): Int = update(
        sql = "UPDATE ${table.qname()}\nSET ${fieldList(namesToFields(fieldNames))}\nWHERE $where",
        params = params,
    )

    context(_: Connection, _: Listener)
    suspend fun update(
        fieldNames: Iterable<String>,
        where: String,
        params: Collection<Param>,
    ): Int = update(
        "UPDATE ${table.qname()}\nSET ${fieldList(namesToFields(fieldNames))}\nWHERE $where",
        params,
    )

    fun namesToFields(names: Iterable<String>): List<Field> =
        names.map { fieldMap[it] ?: error("Unknown field \"$it\"") }

    companion object {
        private fun parameterList(fields: Iterable<Field>): String =
            fields.joinToString(", ") {
                when (val sqlType = it.sqlType) {
                    null -> "?"
                    else -> "? :: $sqlType"
                }
            }

        private fun fieldList(fields: Iterable<Field>): String =
            fields.joinToString(", ") {
                when (val sqlType = it.sqlType) {
                    null -> "\"${it.name}\" = ?"
                    else -> "\"${it.name}\" = ? :: $sqlType"
                }
            }
    }
}

/** This is used to produce param lists from source records on demand. */
private inline fun <P, Q> Iterable<P>.transformer(crossinline f: (P) -> Q): Iterator<Q> {
    val it = iterator()
    return object : Iterator<Q> {
        override fun next(): Q = f(it.next())

        override fun hasNext(): Boolean = it.hasNext()
    }
}
