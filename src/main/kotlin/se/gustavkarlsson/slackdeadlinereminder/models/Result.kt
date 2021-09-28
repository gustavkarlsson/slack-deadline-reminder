package se.gustavkarlsson.slackdeadlinereminder.models

sealed interface Result {
    data class Inserted(val deadline: Deadline) : Result

    data class Deadlines(val deadlines: List<Deadline>) : Result

    data class Removed(val deadline: Deadline) : Result

    data class RemoveFailed(val id: Int) : Result
}
