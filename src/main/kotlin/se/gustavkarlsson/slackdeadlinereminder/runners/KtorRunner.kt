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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.merge
import se.gustavkarlsson.slackdeadlinereminder.CommandProcessor
import se.gustavkarlsson.slackdeadlinereminder.ReminderSource
import se.gustavkarlsson.slackdeadlinereminder.Runner
import se.gustavkarlsson.slackdeadlinereminder.command.CommandParser
import se.gustavkarlsson.slackdeadlinereminder.command.CommandParserFailureFormatter
import se.gustavkarlsson.slackdeadlinereminder.command.CommandResponseFormatter
import se.gustavkarlsson.slackdeadlinereminder.models.OutgoingMessage
import com.slack.api.bolt.App as BoltApp
import com.slack.api.bolt.response.Response as BoltResponse

class KtorRunner(
    private val app: CommandProcessor,
    private val reminderSource: ReminderSource,
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

    override suspend fun run(): Nothing = coroutineScope {
        launch { scheduleReminders() }
        withContext(Dispatchers.Default) {
            runServer()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun scheduleReminders(): Nothing {
        coroutineScope {
            merge(reminderSource.reminders, commandResponseMessages).collect { message ->
                launch {
                    // FIXME handle exceptions
                    methods.chatPostMessage { req ->
                        req.channel(message.channelId.value)
                            .text(message.text)
                    }
                }
            }
        }
        error("Collection finished unexpectedly")
    }

    private fun runServer(): Nothing {
        val server = embeddedServer(Netty, port = port, host = address) {
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
        }
        server.start(wait = true)
        error("Server stopped unexpectedly")
    }

    private suspend fun handleSlashCommand(payload: SlashCommandPayload): BoltResponse {
        val command = when (val parseResult = commandParser.parse(payload.text)) {
            is CommandParser.Success -> parseResult.command
            is CommandParser.Failure -> {
                val text = commandParserFailureFormatter.format(parseResult)
                return BoltResponse.ok(text)
            }
        }
        val context = payload.toMessageContext()
        val result = app.process(context, command)
        val text = commandResponseFormatter.format(result)
        when (result) {
            is CommandProcessor.Deadlines, is CommandProcessor.RemoveFailed -> Unit
            is CommandProcessor.Inserted, is CommandProcessor.Removed -> {
                val message = OutgoingMessage(context.channelId, text)
                commandResponseMessages.emit(message)
            }
        }
        return BoltResponse.ok(text)
    }
}
