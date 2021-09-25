package se.gustavkarlsson.slackdeadlinereminder.ktor

import com.slack.api.bolt.App
import com.slack.api.bolt.ktor.toBoltRequest
import com.slack.api.bolt.request.builtin.SlashCommandRequest
import com.slack.api.bolt.util.SlackRequestParser
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import se.gustavkarlsson.slackdeadlinereminder.ktor.handleSlashCommand

fun serveWithKtor(app: App) {
    val slackRequestParser = SlackRequestParser(app.config())
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        routing {
            post("/") {
                val boltRequest = toBoltRequest(call, slackRequestParser)
                if (boltRequest !is SlashCommandRequest) {
                    call.respond(HttpStatusCode.UnprocessableEntity)
                } else {
                    handleSlashCommand(call, boltRequest.payload)
                }
            }
        }
    }.start(wait = true)
}
