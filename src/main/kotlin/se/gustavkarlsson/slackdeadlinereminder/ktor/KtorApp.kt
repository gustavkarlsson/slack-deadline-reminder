package se.gustavkarlsson.slackdeadlinereminder.ktor

import com.slack.api.app_backend.slash_commands.payload.SlashCommandPayload
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import se.gustavkarlsson.slackdeadlinereminder.app.App
import se.gustavkarlsson.slackdeadlinereminder.command.CommandParser
import se.gustavkarlsson.slackdeadlinereminder.command.CommandParserFailureFormatter
import se.gustavkarlsson.slackdeadlinereminder.command.CommandResponseFormatter
import com.slack.api.bolt.App as BoltApp
import com.slack.api.bolt.response.Response as BoltResponse

class KtorApp(
    private val app: App,
    private val commandParser: CommandParser,
    private val commandResponseFormatter: CommandResponseFormatter,
    private val commandParserFailureFormatter: CommandParserFailureFormatter,
) {
    // Expects env variables (SLACK_BOT_TOKEN, SLACK_SIGNING_SECRET)
    private val boltApp = BoltApp()
    private val methods = boltApp.slack.methods("FIXME")
    private val slackRequestParser = SlackRequestParser(boltApp.config())

    suspend fun run() = coroutineScope {
        launch { scheduleReminders() }
        runServer()
    }

    private suspend fun scheduleReminders() = coroutineScope {
        app.scheduleReminders().collect { deadline ->
            val text = buildString {
                append("Reminder: ")
                append("'${deadline.name}'")
                append(" is due ")
                append(deadline.date.toString())
            }
            launch {
                methods.chatPostMessage { req ->
                    req.channel(deadline.channel)
                        .text(text)
                }
            }
        }
    }

    private fun CoroutineScope.runServer() {
        embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
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
        return BoltResponse.ok(text)
    }
}
