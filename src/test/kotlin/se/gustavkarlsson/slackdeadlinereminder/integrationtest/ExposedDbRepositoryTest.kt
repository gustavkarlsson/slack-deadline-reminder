package se.gustavkarlsson.slackdeadlinereminder.integrationtest

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import se.gustavkarlsson.slackdeadlinereminder.models.ChannelId
import se.gustavkarlsson.slackdeadlinereminder.models.DatabaseConfig
import se.gustavkarlsson.slackdeadlinereminder.models.UserId
import se.gustavkarlsson.slackdeadlinereminder.repo.ExposedDbRepository
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ExposedDbRepositoryTest : IntegrationTest() {

    private val config: DatabaseConfig.Postgres
        get() {
            val dbAddress = with(postgresqlContainer) {
                "$host:$firstMappedPort/$databaseName"
            }
            return DatabaseConfig.Postgres(
                dbAddress,
                postgresqlContainer.username,
                postgresqlContainer.password,
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private val repository: ExposedDbRepository = ExposedDbRepository(config)

    @Test
    fun `there are no deadlines initially`() = runTest(testDispatcher) {
        assertEquals(0, repository.list().size)
    }

    @Test
    fun `contains one entry after insertion`() = runTest(testDispatcher) {
        val deadline = repository.insert(
            UserId("Gustav"),
            ChannelId("General"),
            LocalDate.of(2021, 12, 18),
            "Deploy to production on friday"
        )

        assertEquals(deadline, repository.list().first())
    }
}
