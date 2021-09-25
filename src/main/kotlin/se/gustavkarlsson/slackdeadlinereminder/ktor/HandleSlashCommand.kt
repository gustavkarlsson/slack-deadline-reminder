package se.gustavkarlsson.slackdeadlinereminder.ktor

import com.slack.api.app_backend.slash_commands.payload.SlashCommandPayload
import io.ktor.application.ApplicationCall

suspend fun handleSlashCommand(call: ApplicationCall, payload: SlashCommandPayload) {

    TODO("Not yet implemented")
}

/*

/deadline add october 10 Release WLA RMV

/deadline list

/deadline remove 5

*/
