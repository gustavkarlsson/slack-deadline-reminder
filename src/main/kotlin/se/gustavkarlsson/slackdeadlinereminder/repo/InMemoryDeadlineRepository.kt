package se.gustavkarlsson.slackdeadlinereminder.repo

import se.gustavkarlsson.slackdeadlinereminder.models.Deadline
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

class InMemoryDeadlineRepository : DeadlineRepository {
    private val nextId = AtomicInteger(1)
    private val data = mutableListOf<Deadline>()

    override suspend fun insert(owner: String, channel: String, date: LocalDate, name: String): Deadline {
        val id = nextId.getAndIncrement()
        val deadline = Deadline(id, owner, channel, date, name)
        data += deadline
        return deadline
    }

    override suspend fun list(): List<Deadline> {
        return data.toList()
    }

    override suspend fun remove(id: Int): Boolean {
        return data.removeIf { it.id == id }
    }
}
