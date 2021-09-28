package se.gustavkarlsson.slackdeadlinereminder.app

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import se.gustavkarlsson.slackdeadlinereminder.models.Command
import se.gustavkarlsson.slackdeadlinereminder.models.Deadline
import se.gustavkarlsson.slackdeadlinereminder.models.Result
import se.gustavkarlsson.slackdeadlinereminder.repo.DeadlineRepository

class App(private val repository: DeadlineRepository) {
    suspend fun handleCommand(userName: String, channelName: String, command: Command): Result {
        return when (command) {
            is Command.Insert -> {
                val deadline = repository.insert(
                    owner = userName,
                    channel = channelName,
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

    suspend fun scheduleReminders(): Flow<Deadline> {
        return emptyFlow()
    }
}
