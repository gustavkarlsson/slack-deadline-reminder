package se.gustavkarlsson.slackdeadlinereminder.command

import se.gustavkarlsson.slackdeadlinereminder.CommandProcessor

object CommandResponseFormatter {
    fun format(result: CommandProcessor.Result): String = when (result) {
        is CommandProcessor.Deadlines -> {
            buildString {
                append("Deadlines:")
                for (deadline in result.deadlines) {
                    appendLine()
                    append("${deadline.id} | ${deadline.text} (${deadline.date})")
                }
            }
        }
        is CommandProcessor.RemoveFailed -> "No deadline found with id: ${result.id}"
        is CommandProcessor.Inserted -> "Added deadline: '${result.deadline.text}' on ${result.deadline.date}"
        is CommandProcessor.Removed -> "Removed deadline: '${result.deadline.text}'"
    }
}
