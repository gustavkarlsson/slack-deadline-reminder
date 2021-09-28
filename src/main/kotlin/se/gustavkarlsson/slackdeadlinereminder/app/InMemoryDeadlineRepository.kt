package se.gustavkarlsson.slackdeadlinereminder.app

import se.gustavkarlsson.slackdeadlinereminder.domain.Deadline
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

class InMemoryDeadlineRepository : DeadlineRepository {
    private val nextId = AtomicInteger(1)
    private val data = mutableListOf<Deadline>()

    override suspend fun insert(owner: String, date: LocalDate, name: String): Deadline {
        val id = nextId.getAndIncrement()
        val deadline = Deadline(id, owner, date, name)
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
