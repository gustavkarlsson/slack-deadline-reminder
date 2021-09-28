package se.gustavkarlsson.slackdeadlinereminder.cli

import arrow.core.getOrHandle
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import se.gustavkarlsson.slackdeadlinereminder.app.App
import se.gustavkarlsson.slackdeadlinereminder.command.CommandParser
import se.gustavkarlsson.slackdeadlinereminder.models.Response

class CliApp(
    private val app: App,
    private val commandParser: CommandParser,
    private val commandName: String,
    private val user: String,
) {

    suspend fun run() = coroutineScope {
        launch { scheduleReminders() }
        do {
            val line = readLine()
            if (line != null) {
                processLine(line)
            }
        } while (line != null)
    }

    private suspend fun scheduleReminders() = coroutineScope {
        app.scheduleReminders().collect { deadline ->
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
        val split = line.split(Regex("\\s+"))
        val commandPart = split.getOrNull(0)
        val textPart = split.getOrNull(1)
        when {
            commandPart?.startsWith('/') == false -> return
            commandPart?.startsWith("/$commandName") == false -> {
                println("Unknown command: $commandPart")
                return
            }
        }
        if (textPart == null) {
            println("No command text")
            return
        }
        val parseResult = commandParser.parse(textPart)
        val command = parseResult.getOrHandle { e ->
            e.printStackTrace()
            return
        }
        val text = when (val response = app.handleCommand(user, command)) {
            is Response.Deadlines -> {
                buildString {
                    appendLine("Deadlines:")
                    for (deadline in response.deadlines) {
                        appendLine("${deadline.id} | ${deadline.name} (${deadline.date})")
                    }
                }
            }
            is Response.Error -> response.message
            is Response.Inserted -> "Inserted: ${response.deadline}"
            Response.Removed -> "Removed"
        }
        println(text)
    }
}
