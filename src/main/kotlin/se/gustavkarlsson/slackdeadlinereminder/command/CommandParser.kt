package se.gustavkarlsson.slackdeadlinereminder.command

import arrow.core.Either
import com.zoho.hawking.HawkingTimeParser
import com.zoho.hawking.datetimeparser.configuration.HawkingConfiguration
import com.zoho.hawking.language.english.model.ParserOutput
import se.gustavkarlsson.slackdeadlinereminder.models.Command
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class CommandParser {
    fun parse(text: String): Either<ParseException, Command> {
        val split = text.split(Regex("\\s+"), limit = 2)
        return Either.catch(::ParseException) { parseSubcommand(split) }
    }

    private fun parseSubcommand(split: List<String>): Command {
        val rest = split.getOrNull(1)
        return when (split.firstOrNull()?.lowercase()) {
            "add" -> parseAdd(requireNotNull(rest))
            "list" -> Command.List
            "remove" -> parseRemove(requireNotNull(rest))
            else -> error("No subcommand entered")
        }
    }

    private fun parseAdd(text: String): Command.Insert {
        val match = parseTime(text)
        val date = getDate(match)
        val replacementString = "\u0000"
        val name = match.recognizerOutputs.fold(text) { acc, output ->
            val replacement = replacementString.repeat(output.text.length)
            val startIndex = match.parserStartIndex + output.recognizerStartIndex
            val endIndex = match.parserStartIndex + output.recognizerEndIndex
            acc.take(startIndex) + replacement + acc.drop(endIndex)
        }
            .replace(replacementString, "")
            .replace(Regex("\\s+"), " ")
            .trim()
        return Command.Insert(date, name)
    }

    private fun parseTime(text: String): ParserOutput {
        val parser = HawkingTimeParser()
        val config = HawkingConfiguration()
        val now = Instant.now()
        val datesFound = parser.parse(text, Date.from(now), config, "eng")
        return datesFound.parserOutputs.first()
    }

    private fun getDate(match: ParserOutput): LocalDate {
        return LocalDateTime.parse(match.dateRange.startDateFormat).toLocalDate()
    }

    private fun parseRemove(text: String): Command.Remove {
        val id = text.toInt()
        return Command.Remove(id)
    }
}

class ParseException(cause: Throwable) : Exception(cause)
