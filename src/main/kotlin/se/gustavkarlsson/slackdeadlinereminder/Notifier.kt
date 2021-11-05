package se.gustavkarlsson.slackdeadlinereminder

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import se.gustavkarlsson.slackdeadlinereminder.models.Deadline
import se.gustavkarlsson.slackdeadlinereminder.repo.DeadlineRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

private val alertTime = LocalTime.of(9, 0)

class Notifier(private val repository: DeadlineRepository) {

    val notifications: Flow<Deadline> = flow {
        while (true) {
            val deadlines = repository.list()
            // FIXME what is a reminder?
            val reminders = listOf<Deadline>()
            for (reminder in reminders) {
                emit(reminder)
            }
            delay(getDelayUntilNextAlertMillis())
        }
    }

    private fun getDelayUntilNextAlertMillis(): Long {
        val now = LocalDateTime.now()
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val todaysAlertTime = today.atTime(alertTime)
        val nextAlert = if (now < todaysAlertTime) {
            todaysAlertTime
        } else {
            tomorrow.atTime(alertTime)
        }
        return now.until(nextAlert, ChronoUnit.MILLIS)
    }
}
