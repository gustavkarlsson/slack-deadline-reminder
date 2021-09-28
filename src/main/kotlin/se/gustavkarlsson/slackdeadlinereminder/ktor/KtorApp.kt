package se.gustavkarlsson.slackdeadlinereminder.ktor

import arrow.core.getOrHandle
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
import se.gustavkarlsson.slackdeadlinereminder.models.Response
import com.slack.api.bolt.App as BoltApp
import com.slack.api.bolt.response.Response as BoltResponse

class KtorApp(
    private val app: App,
    private val commandParser: CommandParser,
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
        val parseResult = commandParser.parse(payload.text)
        val command = parseResult.getOrHandle { throw it }
        return when (val response = app.handleCommand(payload.userName, command)) {
            is Response.Deadlines -> {
                val text = buildString {
                    appendLine("Deadlines:")
                    for (deadline in response.deadlines) {
                        appendLine("${deadline.id} | ${deadline.name} (${deadline.date})")
                    }
                }
                BoltResponse.ok(text)
            }
            is Response.Error -> BoltResponse.error(HttpStatusCode.UnprocessableEntity.value)
            is Response.Inserted -> BoltResponse.ok()
            Response.Removed -> BoltResponse.ok()
        }
    }
}
