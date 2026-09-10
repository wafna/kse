package wafna.kse.common

import org.slf4j.LoggerFactory
import wafna.kse.LoggingListener
import wafna.kse.Param

private val log = LoggerFactory.getLogger(TestLoggingListener::class.java)

object TestLoggingListener : LoggingListener(log) {
    override fun execute(sql: String) {
        info { "EXECUTE SQL\n$sql" }
    }

    override fun select(sql: String, params: List<Param>) {
        info { "SELECT SQL\n$sql${params.showParams()}" }
    }

    override fun insert(sql: String) {
        info { "INSERT SQL\n$sql" }
    }

    override fun insertRecord(values: List<Param>) {
        info { "INSERT RECORD${values.showParams()}" }
    }

    override fun update(sql: String, params: List<Param>) {
        info { "UPDATE SQL\n$sql${params.showParams()}" }
    }
}

private fun List<Param>.showParams() = withIndex().joinToString("") { "\n\t[${1 + it.index}] ${it.value.inspect()}" }