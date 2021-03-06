package se.gustavkarlsson.slackdeadlinereminder.command

import se.gustavkarlsson.slackdeadlinereminder.nlp.NlpDateParser

// FIXME extract interface
class CommandParser(private val nlpDateParser: NlpDateParser) {
    suspend fun parse(text: String): Result {
        val split = text.trim().split(Regex("\\s+"), limit = 2)
        val subcommand = split.getOrNull(0)
        val argument = split.getOrNull(1)
        return parseSubcommand(subcommand, argument)
    }

    private suspend fun parseSubcommand(subcommand: String?, argument: String?): Result {
        return when (subcommand?.lowercase()) {
            "add" -> {
                if (argument == null) {
                    Failure.MissingSubcommandArgument
                } else {
                    parseAdd(argument)
                }
            }
            "remove" -> {
                if (argument == null) {
                    Failure.MissingSubcommandArgument
                } else {
                    parseRemove(argument)
                }
            }
            "list" -> Success(Command.List)
            else -> Failure.MissingSubcommand
        }
    }

    private suspend fun parseAdd(text: String): Result {
        val command = when (val result = nlpDateParser.parse(text)) {
            is NlpDateParser.Success -> Command.Insert(result.date, result.remainingText)
            NlpDateParser.Failure.TimeNotFound -> return Failure.MissingTime
        }
        return Success(command)
    }

    private fun parseRemove(text: String): Result {
        val id = text.trim().toIntOrNull()
        return if (id != null) {
            Success(Command.Remove(id))
        } else {
            Failure.MissingId
        }
    }

    sealed interface Result
    data class Success(val command: Command) : Result
    sealed interface Failure : Result {
        object MissingSubcommand : Failure
        object MissingSubcommandArgument : Failure
        object MissingTime : Failure
        object MissingId : Failure
    }
}
