package se.gustavkarlsson.slackdeadlinereminder.models

import java.time.LocalDate

data class Deadline(
    val id: Int,
    val owner: String,
    val channel: String,
    val date: LocalDate,
    val name: String,
)
