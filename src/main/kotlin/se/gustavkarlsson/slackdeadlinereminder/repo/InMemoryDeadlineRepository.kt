package se.gustavkarlsson.slackdeadlinereminder.repo

import se.gustavkarlsson.slackdeadlinereminder.models.Deadline
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

class InMemoryDeadlineRepository : DeadlineRepository {
    private val nextId = AtomicInteger(1)
    private val data = mutableListOf<Deadline>()

    override suspend fun insert(ownerUserName: String, channelName: String, date: LocalDate, name: String): Deadline {
        val id = nextId.getAndIncrement()
        val deadline = Deadline(id, ownerUserName, channelName, date, name)
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
