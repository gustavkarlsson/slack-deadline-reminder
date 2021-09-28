package se.gustavkarlsson.slackdeadlinereminder.cli

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import se.gustavkarlsson.slackdeadlinereminder.app.App
import se.gustavkarlsson.slackdeadlinereminder.command.CommandParser
import se.gustavkarlsson.slackdeadlinereminder.models.Result

class CliApp(
    private val app: App,
    private val commandParser: CommandParser,
    private val commandName: String,
    private val userName: String,
    private val channelName: String,
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
        val split = line.split(Regex("\\s+"), limit = 2)
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
        val command = when (val parseResult = commandParser.parse(textPart)) {
            is CommandParser.Result.Success -> parseResult.command
            is CommandParser.Result.Failure -> {
                println("Error: $parseResult")
                return
            }
        }
        val text = when (val response = app.handleCommand(userName, channelName, command)) {
            is Result.Deadlines -> {
                buildString {
                    appendLine("Deadlines:")
                    for (deadline in response.deadlines) {
                        appendLine("${deadline.id} | ${deadline.name} (${deadline.date})")
                    }
                }
            }
            is Result.RemoveFailed -> "No deadline found with id: ${response.id}"
            is Result.Inserted -> "Added deadline on ${response.deadline.date}"
            is Result.Removed -> "Removed deadline: '${response.deadline.name}'"
        }
        println(text)
    }
}
