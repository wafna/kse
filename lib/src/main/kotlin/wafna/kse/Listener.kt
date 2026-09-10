package wafna.kse

import org.slf4j.Logger

/**
 * Listens to actions performed by the database.
 */
interface Listener {
    fun execute(sql: String)
    fun select(sql: String, params: List<Param>)
    fun insert(sql: String)
    /**
     * Since records are generated lazily on inserts, they come here individually.
     */
    fun insertRecord(values: List<Param>)
    fun update(sql: String, params: List<Param>)
}

/**
 * Does nothing; the default.
 */
object ListenerNOOP : Listener {
    override fun execute(sql: String) {}
    override fun select(sql: String, params: List<Param>) {}
    override fun insert(sql: String) {}
    override fun insertRecord(values: List<Param>) {}
    override fun update(sql: String, params: List<Param>) {}
}

/**
 * Lazy logging enabled listener.
 */
@Suppress("unused") // verified by inspection
abstract class LoggingListener(val log: Logger) : Listener {
    fun trace(s: () -> String) {
        if (log.isTraceEnabled) log.trace(s())
    }

    fun debug(s: () -> String) {
        if (log.isDebugEnabled) log.debug(s())
    }

    fun info(s: () -> String) {
        if (log.isInfoEnabled) log.info(s())
    }

    fun warn(s: () -> String) {
        if (log.isWarnEnabled) log.warn(s())
    }

    fun error(s: () -> String) {
        if (log.isErrorEnabled) log.error(s())
    }
}

