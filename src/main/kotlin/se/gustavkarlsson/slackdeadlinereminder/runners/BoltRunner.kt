package se.gustavkarlsson.slackdeadlinereminder.runners

import com.slack.api.app_backend.slash_commands.payload.SlashCommandPayload
import com.slack.api.bolt.jetty.SlackAppServer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import se.gustavkarlsson.slackdeadlinereminder.App
import se.gustavkarlsson.slackdeadlinereminder.Runner
import se.gustavkarlsson.slackdeadlinereminder.command.CommandParser
import se.gustavkarlsson.slackdeadlinereminder.command.CommandParserFailureFormatter
import se.gustavkarlsson.slackdeadlinereminder.command.CommandResponseFormatter
import se.gustavkarlsson.slackdeadlinereminder.models.OutgoingMessage
import se.gustavkarlsson.slackdeadlinereminder.models.Result
import com.slack.api.bolt.App as BoltApp

class BoltRunner(
    private val app: App,
    private val commandParser: CommandParser,
    private val commandResponseFormatter: CommandResponseFormatter,
    private val commandParserFailureFormatter: CommandParserFailureFormatter,
    private val commandName: String,
) : Runner {
    // Expects env variables (SLACK_BOT_TOKEN, SLACK_SIGNING_SECRET)
    private val boltApp = BoltApp()
    private val methods = boltApp.slack.methods("FIXME")
    private val commandResponseMessages = MutableSharedFlow<OutgoingMessage>(extraBufferCapacity = 8)

    override suspend fun run() = coroutineScope {
        launch { scheduleReminders() }
        boltApp.command(commandName) { req, ctx ->
            val response = runBlocking {
                handleSlashCommand(req.payload)
            }
            ctx.ack(response)
        }
        SlackAppServer(boltApp).start()
    }

    private suspend fun scheduleReminders() {
        val reminderMessages: Flow<OutgoingMessage> = app.reminders.map { deadline ->
            val text = buildString {
                append("Reminder: ")
                append("'${deadline.name}'")
                append(" is due ")
                append(deadline.date.toString())
            }
            OutgoingMessage(deadline.channelName, text)
        }
        coroutineScope {
            merge(reminderMessages, commandResponseMessages).collect { message ->
                launch {
                    methods.chatPostMessage { req ->
                        req.channel(message.channelName)
                            .text(message.text)
                    }
                }
            }
        }
    }

    private suspend fun handleSlashCommand(payload: SlashCommandPayload): String {
        val command = when (val parseResult = commandParser.parse(payload.text)) {
            is CommandParser.Result.Success -> parseResult.command
            is CommandParser.Result.Failure -> {
                return commandParserFailureFormatter.format(parseResult)
            }
        }
        val result = app.handleCommand(payload.userName, payload.channelName, command)
        val text = commandResponseFormatter.format(result)
        return when (result) {
            is Result.Deadlines, is Result.RemoveFailed -> {
                text
            }
            is Result.Inserted, is Result.Removed -> {
                val message = OutgoingMessage(payload.channelName, text)
                commandResponseMessages.emit(message)
                text
            }
        }
    }
}
