package se.gustavkarlsson.slackdeadlinereminder

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import se.gustavkarlsson.slackdeadlinereminder.models.Deadline
import se.gustavkarlsson.slackdeadlinereminder.models.OutgoingMessage
import se.gustavkarlsson.slackdeadlinereminder.repo.DeadlineRepository
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

// FIXME extract interface
class ReminderSource(
    private val repository: DeadlineRepository,
    private val clock: Clock,
    private val reminderTime: LocalTime,
    reminderDurations: Set<Duration>,
) {
    val reminders: Flow<OutgoingMessage> = flow {
        if (missedTodaysReminders) {
            emitReminders()
        }
        while (true) {
            delay(timeUntilNextReminder)
            emitReminders()
        }
    }

    private val missedTodaysReminders: Boolean
        get() {
            val currentTimeOfDay = LocalTime.now(clock)
            return currentTimeOfDay.isAfter(reminderTime)
        }

    private suspend fun FlowCollector<OutgoingMessage>.emitReminders() {
        val messages = getReminderMessages(repository.list())
        emitAll(messages.asFlow())
    }

    private val timeUntilNextReminder: Long
        get() {
            val currentTime = LocalDateTime.now(clock)
            return getTimeUntilNextReminder(currentTime, reminderTime)
        }
}

private fun getTimeUntilNextReminder(now: LocalDateTime, reminderTime: LocalTime): Long {
    val today = now.toLocalDate()
    val todaysReminderTime = today.atTime(reminderTime)
    val tomorrowsReminderTime = today.plusDays(1).atTime(reminderTime)
    val nextReminderTime = if (now.isBefore(todaysReminderTime)) {
        todaysReminderTime
    } else {
        tomorrowsReminderTime
    }
    return now.until(nextReminderTime, ChronoUnit.MILLIS)
}

private fun getReminderMessages(deadlines: List<Deadline>): List<OutgoingMessage> {
    return deadlines
        .filter(::shouldRemind)
        .groupBy { it.channelId }
        .mapValues { (_, deadlines) ->
            deadlines.sortedBy { it.date }
        }
        .map { (channelId, deadlines) ->
            val text = buildString {
                append("Reminder:")
                val padLength = deadlines.maxOf { it.id.toString().length } + 1
                for (deadline in deadlines) {
                    appendLine()
                    append(deadline.id.toString().padEnd(padLength))
                    append("| '${deadline.text}' is due ${deadline.date}") // Extract to shared formatter (shared with ReminderSource)
                }
            }
            OutgoingMessage(channelId, text)
        }
}

private fun shouldRemind(deadline: Deadline): Boolean {
    TODO("FIXME implement")
}

private fun Deadline.toMessage(): OutgoingMessage {
    val text = buildString {
        append("Reminder: ")
        append("'${text}'")
        append(" is due ")
        append(date.toString())
    }
    return OutgoingMessage(channelId, text)
}
