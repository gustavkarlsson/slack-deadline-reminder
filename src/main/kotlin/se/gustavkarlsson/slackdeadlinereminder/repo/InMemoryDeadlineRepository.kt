package se.gustavkarlsson.slackdeadlinereminder.repo

import se.gustavkarlsson.slackdeadlinereminder.models.ChannelId
import se.gustavkarlsson.slackdeadlinereminder.models.Deadline
import se.gustavkarlsson.slackdeadlinereminder.models.UserId
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

class InMemoryDeadlineRepository : DeadlineRepository {
    private val nextId = AtomicInteger(1)
    private val data = mutableListOf<Deadline>()

    override suspend fun insert(ownerId: UserId, channelId: ChannelId, date: LocalDate, text: String): Deadline {
        val id = nextId.getAndIncrement()
        val deadline = Deadline(id, ownerId, channelId, date, text)
        data += deadline
        return deadline
    }

    override suspend fun list(): List<Deadline> {
        return data.toList()
    }

    override suspend fun remove(id: Int): Deadline? {
        val deadline = data.find { it.id == id }
        if (deadline != null) {
            data.remove(deadline)
        }
        return deadline
    }
}
