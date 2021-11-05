package se.gustavkarlsson.slackdeadlinereminder.models

import java.time.LocalDate

data class Deadline(
    val id: Int,
    val ownerId: UserId,
    val channelId: ChannelId,
    val date: LocalDate,
    val name: String, // FIXME Rename to text
)
