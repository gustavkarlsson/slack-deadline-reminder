package se.gustavkarlsson.slackdeadlinereminder.models

data class MessageContext(
    val userId: UserId,
    val channelId: ChannelId,
)
