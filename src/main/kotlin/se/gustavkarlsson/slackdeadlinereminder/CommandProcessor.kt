package se.gustavkarlsson.slackdeadlinereminder

import se.gustavkarlsson.slackdeadlinereminder.models.Command
import se.gustavkarlsson.slackdeadlinereminder.models.Deadline
import se.gustavkarlsson.slackdeadlinereminder.models.MessageContext
import se.gustavkarlsson.slackdeadlinereminder.repo.DeadlineRepository

class CommandProcessor(private val repository: DeadlineRepository) {
    suspend fun process(messageContext: MessageContext, command: Command): Result {
        return when (command) {
            is Command.Insert -> {
                val deadline = repository.insert(
                    ownerId = messageContext.userId,
                    channelId = messageContext.channelId,
                    date = command.date,
                    text = command.text,
                )
                Inserted(deadline)
            }
            Command.List -> {
                val deadlines = repository.list()
                Deadlines(deadlines)
            }
            is Command.Remove -> {
                val removed = repository.remove(command.id)
                if (removed != null) {
                    Removed(removed)
                } else {
                    RemoveFailed(command.id)
                }
            }
        }
    }

    sealed interface Result
    data class Inserted(val deadline: Deadline) : Result
    data class Deadlines(val deadlines: List<Deadline>) : Result
    data class Removed(val deadline: Deadline) : Result
    data class RemoveFailed(val id: Int) : Result
}
