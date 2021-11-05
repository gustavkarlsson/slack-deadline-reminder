package se.gustavkarlsson.slackdeadlinereminder.command

import se.gustavkarlsson.slackdeadlinereminder.models.Result

object CommandResponseFormatter {
    fun format(result: Result): String = when (result) {
        is Result.Deadlines -> {
            buildString {
                append("Deadlines:")
                for (deadline in result.deadlines) {
                    appendLine()
                    append("${deadline.id} | ${deadline.text} (${deadline.date})")
                }
            }
        }
        is Result.RemoveFailed -> "No deadline found with id: ${result.id}"
        is Result.Inserted -> "Added deadline: '${result.deadline.text}' on ${result.deadline.date}"
        is Result.Removed -> "Removed deadline: '${result.deadline.text}'"
    }
}
