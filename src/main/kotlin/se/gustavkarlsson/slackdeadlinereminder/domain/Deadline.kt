package se.gustavkarlsson.slackdeadlinereminder.domain

import java.time.LocalDate

data class Deadline(
    val id: Int,
    val owner: String,
    val date: LocalDate,
    val name: String,
)
