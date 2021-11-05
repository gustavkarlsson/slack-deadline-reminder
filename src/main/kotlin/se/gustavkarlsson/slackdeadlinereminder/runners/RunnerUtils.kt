package se.gustavkarlsson.slackdeadlinereminder.runners

import com.slack.api.app_backend.slash_commands.payload.SlashCommandPayload
import se.gustavkarlsson.slackdeadlinereminder.models.ChannelId
import se.gustavkarlsson.slackdeadlinereminder.models.MessageContext
import se.gustavkarlsson.slackdeadlinereminder.models.UserId

fun SlashCommandPayload.toMessageContext() = MessageContext(
    userId = UserId(userId),
    channelId = ChannelId(channelId),
)
