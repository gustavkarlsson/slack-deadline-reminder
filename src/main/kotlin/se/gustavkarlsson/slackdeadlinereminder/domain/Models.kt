package se.gustavkarlsson.slackdeadlinereminder.domain

import se.gustavkarlsson.slackdeadlinereminder.app.Deadline
import java.time.LocalDate

data class Reminder(val todo: Nothing)

sealed interface Command {
    data class Insert(
        val date: LocalDate,
        val name: String,
    ) : Command

    object List : Command

    data class Remove(val id: Int) : Command
}

sealed interface Response {
    data class Inserted(val deadline: Deadline) : Response

    data class Deadlines(val deadlines: List<Deadline>) : Response

    object Removed : Response

    data class Error(val message: String) : Response
}
