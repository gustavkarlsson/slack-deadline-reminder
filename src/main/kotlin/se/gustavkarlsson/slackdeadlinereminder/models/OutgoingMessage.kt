package se.gustavkarlsson.slackdeadlinereminder.models

data class OutgoingMessage(val channelId: ChannelId, val text: String)
