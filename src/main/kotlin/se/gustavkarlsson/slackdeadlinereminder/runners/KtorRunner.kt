package se.gustavkarlsson.slackdeadlinereminder.runners

import com.slack.api.app_backend.slash_commands.payload.SlashCommandPayload
import com.slack.api.bolt.AppConfig
import com.slack.api.bolt.ktor.respond
import com.slack.api.bolt.ktor.toBoltRequest
import com.slack.api.bolt.request.builtin.SlashCommandRequest
import com.slack.api.bolt.util.SlackRequestParser
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import se.gustavkarlsson.slackdeadlinereminder.App
import se.gustavkarlsson.slackdeadlinereminder.Runner
import se.gustavkarlsson.slackdeadlinereminder.command.CommandParser
import se.gustavkarlsson.slackdeadlinereminder.command.CommandParserFailureFormatter
import se.gustavkarlsson.slackdeadlinereminder.command.CommandResponseFormatter
import se.gustavkarlsson.slackdeadlinereminder.models.OutgoingMessage
import se.gustavkarlsson.slackdeadlinereminder.models.Result
import com.slack.api.bolt.App as BoltApp
import com.slack.api.bolt.response.Response as BoltResponse

class KtorRunner(
    private val app: App,
    private val commandParser: CommandParser,
    private val commandResponseFormatter: CommandResponseFormatter,
    private val commandParserFailureFormatter: CommandParserFailureFormatter,
    private val address: String,
    private val port: Int,
    slackBotToken: String,
    slackSigningSecret: String,
) : Runner {
    private val boltApp = let {
        val appConfig = AppConfig.builder()
            .singleTeamBotToken(slackBotToken)
            .signingSecret(slackSigningSecret)
            .build()
        BoltApp(appConfig)
    }
    private val methods = boltApp.slack.methods(slackBotToken)
    private val slackRequestParser = SlackRequestParser(boltApp.config())
    private val commandResponseMessages = MutableSharedFlow<OutgoingMessage>(extraBufferCapacity = 8)

    override suspend fun run() = coroutineScope {
        launch { scheduleReminders() }
        runServer()
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

    private fun CoroutineScope.runServer() {
        embeddedServer(Netty, port = port, host = address) {
            routing {
                post("/") {
                    val boltRequest = toBoltRequest(call, slackRequestParser)
                    if (boltRequest !is SlashCommandRequest) {
                        call.respond(HttpStatusCode.UnprocessableEntity)
                    } else {
                        val boltResponse = handleSlashCommand(boltRequest.payload)
                        respond(call, boltResponse)
                    }
                }
            }
        }.start(wait = true)
    }

    private suspend fun handleSlashCommand(payload: SlashCommandPayload): BoltResponse {
        val command = when (val parseResult = commandParser.parse(payload.text)) {
            is CommandParser.Result.Success -> parseResult.command
            is CommandParser.Result.Failure -> {
                val text = commandParserFailureFormatter.format(parseResult)
                return BoltResponse.ok(text)
            }
        }
        val result = app.handleCommand(payload.userName, payload.channelName, command)
        val text = commandResponseFormatter.format(result)
        return when (result) {
            is Result.Deadlines, is Result.RemoveFailed -> {
                BoltResponse.ok(text)
            }
            is Result.Inserted, is Result.Removed -> {
                val message = OutgoingMessage(payload.channelName, text)
                commandResponseMessages.emit(message)
                BoltResponse.ok()
            }
        }
    }
}
