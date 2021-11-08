package se.gustavkarlsson.slackdeadlinereminder.nlp

import java.time.LocalDate

interface NlpDateParser {
    suspend fun parse(text: String): Result

    sealed interface Result
    data class Success(val date: LocalDate, val remainingText: String) : Result
    sealed interface Failure : Result {
        object TimeNotFound : Failure
    }
}
