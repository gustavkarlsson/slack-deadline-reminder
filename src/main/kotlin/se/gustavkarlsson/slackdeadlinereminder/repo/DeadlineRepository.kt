package se.gustavkarlsson.slackdeadlinereminder.repo

import se.gustavkarlsson.slackdeadlinereminder.models.ChannelId
import se.gustavkarlsson.slackdeadlinereminder.models.Deadline
import se.gustavkarlsson.slackdeadlinereminder.models.UserId
import java.time.LocalDate

interface DeadlineRepository {
    suspend fun insert(ownerId: UserId, channelId: ChannelId, date: LocalDate, name: String): Deadline
    suspend fun list(): List<Deadline>
    suspend fun remove(id: Int): Deadline?
}
