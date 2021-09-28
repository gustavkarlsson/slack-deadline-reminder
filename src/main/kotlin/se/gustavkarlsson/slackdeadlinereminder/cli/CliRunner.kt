package se.gustavkarlsson.slackdeadlinereminder.cli

import edu.stanford.nlp.util.logging.Redwood
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import se.gustavkarlsson.slackdeadlinereminder.Runner
import se.gustavkarlsson.slackdeadlinereminder.app.App
import se.gustavkarlsson.slackdeadlinereminder.command.CommandParser
import se.gustavkarlsson.slackdeadlinereminder.command.CommandParserFailureFormatter
import se.gustavkarlsson.slackdeadlinereminder.command.CommandResponseFormatter
import java.io.OutputStream
import java.io.PrintStream

class CliRunner(
    private val app: App,
    private val commandParser: CommandParser,
    private val commandResponseFormatter: CommandResponseFormatter,
    private val commandParserFailureFormatter: CommandParserFailureFormatter,
    private val commandName: String,
    private val userName: String,
    private val channelName: String,
) : Runner {

    override suspend fun run() = coroutineScope {
        disableNlpLogging()
        launch { scheduleReminders() }
        do {
            val line = readLine()
            if (line != null) {
                processLine(line)
            }
        } while (line != null)
    }

    private fun disableNlpLogging() {
        System.setErr(PrintStream(object : OutputStream() {
            override fun write(b: Int) = Unit
        }))
        Redwood.stop()
    }

    private suspend fun scheduleReminders() = coroutineScope {
        app.reminders.collect { deadline ->
            val text = buildString {
                append("Reminder: ")
                append("'${deadline.name}'")
                append(" is due ")
                append(deadline.date.toString())
            }
            println(text)
        }
    }

    private suspend fun processLine(line: String) {
        val split = line.split(Regex("\\s+"), limit = 2)
        val commandPart = split.getOrNull(0).orEmpty()
        val textPart = split.getOrNull(1).orEmpty()
        when {
            !commandPart.startsWith('/') -> return
            !commandPart.startsWith("/$commandName") -> {
                println("$commandPart is not a valid command")
                return
            }
        }
        val command = when (val parseResult = commandParser.parse(textPart)) {
            is CommandParser.Result.Success -> parseResult.command
            is CommandParser.Result.Failure -> {
                val text = commandParserFailureFormatter.format(parseResult)
                println(text)
                return
            }
        }
        val result = app.handleCommand(userName, channelName, command)
        val text = commandResponseFormatter.format(result)
        println(text)
    }
}
