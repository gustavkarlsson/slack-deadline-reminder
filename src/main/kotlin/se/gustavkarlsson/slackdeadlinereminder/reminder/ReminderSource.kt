package se.gustavkarlsson.slackdeadlinereminder.reminder

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import se.gustavkarlsson.slackdeadlinereminder.models.Deadline
import se.gustavkarlsson.slackdeadlinereminder.models.OutgoingMessage
import se.gustavkarlsson.slackdeadlinereminder.repo.DeadlineRepository
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

// FIXME extract interface
class ReminderSource(
    private val repository: DeadlineRepository,
    private val clock: Clock,
    private val reminderTime: LocalTime,
    private val reminderDays: Set<Int>,
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
            val now = LocalTime.now(clock)
            return now.isAfter(reminderTime)
        }

    private suspend fun FlowCollector<OutgoingMessage>.emitReminders() {
        val today = LocalDate.now(clock)
        val messages = getReminderMessages(repository.list(), reminderDays, today)
        emitAll(messages.asFlow())
    }

    private val timeUntilNextReminder: Long
        get() {
            val now = LocalDateTime.now(clock)
            return getTimeUntilNextReminder(now, reminderTime)
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

private fun getReminderMessages(
    deadlines: List<Deadline>,
    reminderDays: Set<Int>,
    today: LocalDate
): List<OutgoingMessage> {
    return deadlines
        .filter { shouldRemind(reminderDays, it.date, today) }
        .groupBy { it.channelId }
        .mapValues { (_, deadlines) ->
            deadlines.sortedBy { it.date }
        }
        .map { (channelId, deadlines) ->
            val text = buildString {
                append("Deadlines:")
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

private fun shouldRemind(reminderDays: Set<Int>, deadlineDate: LocalDate, today: LocalDate): Boolean {
    val reminderDates = reminderDays
        .map { daysDelta ->
            deadlineDate.minusDays(daysDelta.toLong())
        }
    return when {
        deadlineDate.isBefore(today) -> true
        deadlineDate == today -> true
        deadlineDate in reminderDates -> true
        else -> false
    }
}
