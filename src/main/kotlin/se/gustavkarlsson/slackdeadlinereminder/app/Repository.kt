package se.gustavkarlsson.slackdeadlinereminder.app

import java.time.LocalDate

interface Repository {
    suspend fun insert(owner: String, date: LocalDate, name: String): Deadline
    suspend fun list(): List<Deadline>
    suspend fun remove(id: Int): Boolean
}

data class Deadline(
    val id: Int,
    val owner: String,
    val date: LocalDate,
    val name: String,
)
