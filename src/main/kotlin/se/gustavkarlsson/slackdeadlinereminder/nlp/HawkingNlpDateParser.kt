package se.gustavkarlsson.slackdeadlinereminder.nlp

import com.zoho.hawking.HawkingTimeParser
import com.zoho.hawking.datetimeparser.configuration.HawkingConfiguration
import com.zoho.hawking.language.english.model.ParserOutput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.util.*

class HawkingNlpDateParser(private val clock: Clock) : NlpDateParser {
    override suspend fun parse(text: String): NlpDateParser.Result {
        val match = findTime(text) ?: return NlpDateParser.Failure.TimeNotFound
        val date = getDate(match) ?: return NlpDateParser.Failure.TimeNotFound
        val replacementSymbol = "\u0000"
        val remainingText = match.recognizerOutputs.fold(text) { acc, output ->
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
        return NlpDateParser.Success(date, remainingText)
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
}
