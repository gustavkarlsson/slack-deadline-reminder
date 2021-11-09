package se.gustavkarlsson.slackdeadlinereminder.command

import se.gustavkarlsson.slackdeadlinereminder.CommandProcessor

// FIXME extract interface
object CommandResponseFormatter {
    fun format(result: CommandProcessor.Result): String = when (result) {
        is CommandProcessor.Deadlines -> {
            if (result.deadlines.isEmpty()) {
                "There are no deadlines"
            } else {
                buildString {
                    append("Deadlines:")
                    val padLength = result.deadlines.maxOf { it.id.toString().length } + 1
                    for (deadline in result.deadlines) {
                        appendLine()
                        append(deadline.id.toString().padEnd(padLength))
                        append("| '${deadline.text}' is due ${deadline.date}") // Extract to shared formatter (shared with ReminderSource)
                    }
                }
            }
        }
        is CommandProcessor.RemoveFailed -> "No deadline found with id: ${result.id}"
        is CommandProcessor.Inserted -> "Added deadline: '${result.deadline.text}' on ${result.deadline.date}"
        is CommandProcessor.Removed -> "Removed deadline: '${result.deadline.text}'"
    }
}
