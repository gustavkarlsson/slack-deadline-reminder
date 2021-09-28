package se.gustavkarlsson.slackdeadlinereminder.models

sealed interface Response {
    data class Inserted(val deadline: Deadline) : Response

    data class Deadlines(val deadlines: List<Deadline>) : Response

    object Removed : Response

    data class Error(val message: String) : Response
}
