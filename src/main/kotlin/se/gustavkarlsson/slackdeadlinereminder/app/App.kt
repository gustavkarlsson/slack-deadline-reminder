package se.gustavkarlsson.slackdeadlinereminder.app

import kotlinx.coroutines.flow.Flow
import se.gustavkarlsson.slackdeadlinereminder.Notifier
import se.gustavkarlsson.slackdeadlinereminder.models.Command
import se.gustavkarlsson.slackdeadlinereminder.models.Deadline
import se.gustavkarlsson.slackdeadlinereminder.models.Result
import se.gustavkarlsson.slackdeadlinereminder.repo.DeadlineRepository

class App(
    private val repository: DeadlineRepository,
    private val notifier: Notifier,
) {
    suspend fun handleCommand(userName: String, channelName: String, command: Command): Result {
        return when (command) {
            is Command.Insert -> {
                val deadline = repository.insert(
                    ownerUserName = userName,
                    channelName = channelName,
                    date = command.date,
                    name = command.name,
                )
                Result.Inserted(deadline)
            }
            Command.List -> {
                val deadlines = repository.list()
                Result.Deadlines(deadlines)
            }
            is Command.Remove -> {
                val removed = repository.remove(command.id)
                if (removed != null) {
                    Result.Removed(removed)
                } else {
                    Result.RemoveFailed(command.id)
                }
            }
        }
    }

    fun scheduleReminders(): Flow<Deadline> {
        return notifier.notify()
    }
}
