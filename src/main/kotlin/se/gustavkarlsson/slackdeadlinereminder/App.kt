package se.gustavkarlsson.slackdeadlinereminder

import kotlinx.coroutines.flow.Flow
import se.gustavkarlsson.slackdeadlinereminder.models.Command
import se.gustavkarlsson.slackdeadlinereminder.models.Deadline
import se.gustavkarlsson.slackdeadlinereminder.models.MessageContext
import se.gustavkarlsson.slackdeadlinereminder.models.Result
import se.gustavkarlsson.slackdeadlinereminder.repo.DeadlineRepository

class App(
    private val repository: DeadlineRepository,
    notifier: Notifier,
) {
    suspend fun handleCommand(messageContext: MessageContext, command: Command): Result {
        return when (command) {
            is Command.Insert -> {
                val deadline = repository.insert(
                    ownerId = messageContext.userId,
                    channelId = messageContext.channelId,
                    date = command.date,
                    name = command.text,
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

    val reminders: Flow<Deadline> = notifier.notifications
}
