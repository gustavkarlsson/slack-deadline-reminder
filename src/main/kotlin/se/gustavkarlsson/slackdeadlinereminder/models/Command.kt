package se.gustavkarlsson.slackdeadlinereminder.models

import java.time.LocalDate

sealed interface Command {
    data class Insert(
        val date: LocalDate,
        val text: String,
    ) : Command

    object List : Command

    data class Remove(val id: Int) : Command
}
