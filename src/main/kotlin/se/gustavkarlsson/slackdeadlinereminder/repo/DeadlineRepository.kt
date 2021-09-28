package se.gustavkarlsson.slackdeadlinereminder.repo

import se.gustavkarlsson.slackdeadlinereminder.models.Deadline
import java.time.LocalDate

interface DeadlineRepository {
    suspend fun insert(owner: String, date: LocalDate, name: String): Deadline
    suspend fun list(): List<Deadline>
    suspend fun remove(id: Int): Boolean
}
