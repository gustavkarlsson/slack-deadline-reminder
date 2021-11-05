package se.gustavkarlsson.slackdeadlinereminder.repo

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import se.gustavkarlsson.slackdeadlinereminder.models.ChannelId
import se.gustavkarlsson.slackdeadlinereminder.models.Deadline
import se.gustavkarlsson.slackdeadlinereminder.models.UserId
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.io.path.readText
import kotlin.io.path.writeText

class JsonFileRepository(
    private val file: Path,
    private val prettyPrint: Boolean = false,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DeadlineRepository {
    private val json = Json {
        prettyPrint = this@JsonFileRepository.prettyPrint
    }
    private val mutex = Mutex()

    override suspend fun insert(ownerId: UserId, channelId: ChannelId, date: LocalDate, text: String): Deadline {
        mutex.withLock {
            val oldRootModel = read()
            val deadline = Deadline(oldRootModel.nextId, ownerId, channelId, date, text)
            val newRootModel = oldRootModel.copy(
                nextId = oldRootModel.nextId + 1,
                deadlines = oldRootModel.deadlines + deadline.toDeadlineJsonModel(),
            )
            write(newRootModel)
            return deadline
        }
    }

    override suspend fun list(): List<Deadline> {
        mutex.withLock {
            val rootModel = read()
            return rootModel.deadlines.map { it.toDeadline() }
        }
    }

    override suspend fun remove(id: Int): Deadline? {
        mutex.withLock {
            val oldRootModel = read()
            val newDeadlines = oldRootModel.deadlines.map { it.toDeadline() }.toMutableList()
            val index = newDeadlines.indexOfFirst { it.id == id }
            if (index == -1) return null
            val deadline = newDeadlines.removeAt(index)
            val newRootModel = oldRootModel.copy(
                deadlines = newDeadlines.map { it.toDeadlineJsonModel() }
            )
            write(newRootModel)
            return deadline
        }
    }

    private suspend fun read(): RootModel {
        val text = withContext(dispatcher) {
            try {
                @Suppress("BlockingMethodInNonBlockingContext")
                file.readText()
            } catch (e: NoSuchFileException) {
                null
            }
        } ?: return RootModel.EMPTY
        return json.decodeFromString(text)
    }

    private suspend fun write(rootModel: RootModel) {
        val text = json.encodeToString(rootModel)
        withContext(dispatcher) {
            @Suppress("BlockingMethodInNonBlockingContext")
            file.writeText(text)
        }
    }
}

private fun DeadlineJsonModel.toDeadline() = Deadline(
    id = id,
    ownerId = UserId(ownerId),
    channelId = ChannelId(channelId),
    date = LocalDate.parse(date, DateTimeFormatter.ISO_DATE),
    text = text,
)

private fun Deadline.toDeadlineJsonModel() = DeadlineJsonModel(
    id = id,
    ownerId = ownerId.value,
    channelId = channelId.value,
    date = date.format(DateTimeFormatter.ISO_DATE),
    text = text,
)

@Serializable
private data class RootModel(
    val nextId: Int,
    val deadlines: List<DeadlineJsonModel>,
) {
    companion object {
        val EMPTY = RootModel(nextId = 1, deadlines = emptyList())
    }
}

@Serializable
private data class DeadlineJsonModel(
    val id: Int,
    val ownerId: String,
    val channelId: String,
    val date: String,
    val text: String,
)
