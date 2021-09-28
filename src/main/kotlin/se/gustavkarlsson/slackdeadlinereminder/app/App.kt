package se.gustavkarlsson.slackdeadlinereminder.app

import kotlinx.coroutines.flow.Flow
import se.gustavkarlsson.slackdeadlinereminder.models.Command
import se.gustavkarlsson.slackdeadlinereminder.models.Deadline
import se.gustavkarlsson.slackdeadlinereminder.models.Response
import se.gustavkarlsson.slackdeadlinereminder.repo.DeadlineRepository

class App(private val repository: DeadlineRepository) {
    suspend fun handleCommand(user: String, command: Command): Response {
        TODO()
    }

    suspend fun scheduleReminders(): Flow<Deadline> {
        TODO()
    }
}
