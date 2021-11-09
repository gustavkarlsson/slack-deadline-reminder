package se.gustavkarlsson.slackdeadlinereminder.runner

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
import mu.KotlinLogging
import se.gustavkarlsson.slackdeadlinereminder.command.CommandParser
import se.gustavkarlsson.slackdeadlinereminder.command.CommandParserFailureFormatter
import se.gustavkarlsson.slackdeadlinereminder.command.CommandProcessor
import se.gustavkarlsson.slackdeadlinereminder.command.CommandResponseFormatter
import se.gustavkarlsson.slackdeadlinereminder.models.ChannelId
import se.gustavkarlsson.slackdeadlinereminder.models.MessageContext
import se.gustavkarlsson.slackdeadlinereminder.models.OutgoingMessage
import se.gustavkarlsson.slackdeadlinereminder.models.UserId
import se.gustavkarlsson.slackdeadlinereminder.reminder.ReminderSource
import com.slack.api.bolt.App as BoltApp
import com.slack.api.bolt.response.Response as BoltResponse

private val logger = KotlinLogging.logger {}

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
    private val sendDispatcher: CoroutineDispatcher = Dispatchers.IO,
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
        launch(sendDispatcher) {
            sendOutgoingMessages()
        }
        runServer()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun sendOutgoingMessages(): Nothing {
        coroutineScope {
            merge(reminderSource.reminders, commandResponseMessages).collect { message ->
                launch(sendDispatcher) {
                    try {
                        post(message)
                    } catch (e: Exception) {
                        logger.error("Failed to post message", e)
                    }
                }
            }
            error("Collection finished unexpectedly")
        }
    }

    private fun post(message: OutgoingMessage) {
        methods.chatPostMessage { req ->
            req.channel(message.channelId.value)
                .text(message.text)
        }
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

private fun SlashCommandPayload.toMessageContext() = MessageContext(
    userId = UserId(userId),
    channelId = ChannelId(channelId),
)
