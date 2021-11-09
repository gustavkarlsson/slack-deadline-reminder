package se.gustavkarlsson.slackdeadlinereminder.repo

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import se.gustavkarlsson.slackdeadlinereminder.config.DatabaseConfig
import se.gustavkarlsson.slackdeadlinereminder.models.ChannelId
import se.gustavkarlsson.slackdeadlinereminder.models.Deadline
import se.gustavkarlsson.slackdeadlinereminder.models.UserId
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean

private object DatabaseConnections {
    private val connections = hashMapOf<DatabaseConfig, Database>()

    @Synchronized
    operator fun get(config: DatabaseConfig.Postgres): Database {
        return connections.getOrPut(config) {
            connectDatabase(config)
        }
    }

    private fun connectDatabase(config: DatabaseConfig.Postgres): Database = Database.connect(
        url = "jdbc:postgresql://${config.address}",
        driver = "org.postgresql.Driver",
        user = config.userName,
        password = config.password,
    )
}

class ExposedDbRepository(
    private val config: DatabaseConfig.Postgres,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DeadlineRepository {

    private val shouldTryCreateTable = AtomicBoolean(true)

    override suspend fun insert(ownerId: UserId, channelId: ChannelId, date: LocalDate, text: String): Deadline {
        return doTransaction {
            val id = DeadlinesTable.insert { row ->
                row[this.ownerId] = ownerId.value
                row[this.channelId] = channelId.value
                row[this.date] = date
                row[this.text] = text
            } get DeadlinesTable.id
            Deadline(id.value, ownerId, channelId, date, text)
        }
    }

    override suspend fun list(): List<Deadline> {
        return doTransaction {
            DeadlinesTable.selectAll()
                .map { it.toDeadline() }
        }
    }

    override suspend fun remove(id: Int): Deadline? {
        return doTransaction {
            val deadline = DeadlinesTable.select {
                DeadlinesTable.id eq id
            }.limit(1)
                .map { it.toDeadline() }
                .firstOrNull()
            DeadlinesTable.deleteWhere {
                DeadlinesTable.id eq id
            }
            deadline
        }
    }

    private suspend fun <T> doTransaction(block: Transaction.() -> T): T {
        val database = DatabaseConnections[config]
        return newSuspendedTransaction(dispatcher, database) {
            if (shouldTryCreateTable.getAndSet(false)) {
                if (DeadlinesTable.exists()) {
                    SchemaUtils.create(DeadlinesTable)
                }
            }
            block()
        }
    }
}

private object DeadlinesTable : IntIdTable(name = "deadlines", columnName = "id") {
    val ownerId = varchar("ownerId", 100)
    val channelId = varchar("channelId", 100)
    val date = date("date")
    val text = text("text")
}

private fun ResultRow.toDeadline() = Deadline(
    id = this[DeadlinesTable.id].value,
    ownerId = UserId(this[DeadlinesTable.ownerId]),
    channelId = ChannelId(this[DeadlinesTable.channelId]),
    date = this[DeadlinesTable.date],
    text = this[DeadlinesTable.text],
)
