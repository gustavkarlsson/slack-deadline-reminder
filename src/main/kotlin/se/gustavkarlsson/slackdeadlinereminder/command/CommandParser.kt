package se.gustavkarlsson.slackdeadlinereminder.command

import com.zoho.hawking.HawkingTimeParser
import com.zoho.hawking.datetimeparser.configuration.HawkingConfiguration
import com.zoho.hawking.language.english.model.ParserOutput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import se.gustavkarlsson.slackdeadlinereminder.models.Command
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.util.*

class CommandParser(
    private val clock: Clock,
) {
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
                    Result.Failure.MissingSubcommandArgument
                } else {
                    parseAdd(argument)
                }
            }
            "remove" -> {
                if (argument == null) {
                    Result.Failure.MissingSubcommandArgument
                } else {
                    parseRemove(argument)
                }
            }
            "list" -> Result.Success(Command.List)
            else -> Result.Failure.MissingSubcommand
        }
    }

    private suspend fun parseAdd(text: String): Result {
        val match = findTime(text) ?: return Result.Failure.MissingTime
        val date = getDate(match) ?: return Result.Failure.MissingTime
        val replacementSymbol = "\u0000"
        val name = match.recognizerOutputs.fold(text) { acc, output ->
            val replacement = replacementSymbol.repeat(output.text.length)
            val startIndex = match.parserStartIndex + output.recognizerStartIndex
            val endIndex = match.parserStartIndex + output.recognizerEndIndex
            val prefix = acc.take(startIndex)
            val suffix = acc.drop(endIndex)
            prefix + replacement + suffix
        }
            .replace(replacementSymbol, "")
            .replace(Regex("\\s+"), " ") // TODO Can we only replace whitespace around replacements?
            .trim()
        val command = Command.Insert(date, name)
        return Result.Success(command)
    }

    private suspend fun findTime(text: String): ParserOutput? {
        val parser = HawkingTimeParser()
        val config = HawkingConfiguration()
        val now = clock.instant()
        val datesFound = withContext(Dispatchers.IO) {
            parser.parse(text, Date.from(now), config, "eng")
        }
        return datesFound.parserOutputs.firstOrNull()
    }

    private fun getDate(match: ParserOutput): LocalDate? {
        return try {
            LocalDateTime.parse(match.dateRange.startDateFormat).toLocalDate()
        } catch (e: DateTimeParseException) {
            null
        }
    }

    private fun parseRemove(text: String): Result {
        val id = text.trim().toIntOrNull()
        return if (id != null) {
            Result.Success(Command.Remove(id))
        } else {
            Result.Failure.MissingId
        }
    }

    sealed interface Result {
        data class Success(val command: Command) : Result
        sealed interface Failure : Result {
            object MissingSubcommand : Failure
            object MissingSubcommandArgument : Failure
            object MissingTime : Failure
            object MissingId : Failure
        }
    }
}
