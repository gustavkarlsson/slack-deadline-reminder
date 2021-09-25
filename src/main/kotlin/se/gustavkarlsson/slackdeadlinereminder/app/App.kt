package se.gustavkarlsson.slackdeadlinereminder.app

import kotlinx.coroutines.flow.Flow
import se.gustavkarlsson.slackdeadlinereminder.domain.Command
import se.gustavkarlsson.slackdeadlinereminder.domain.Reminder
import se.gustavkarlsson.slackdeadlinereminder.domain.Response

class App(private val repository: Repository) {
    suspend fun handleCommand(user: String, command: Command): Response {
        TODO()
    }

    suspend fun scheduleReminders(): Flow<Reminder> {
        TODO()
    }
}
