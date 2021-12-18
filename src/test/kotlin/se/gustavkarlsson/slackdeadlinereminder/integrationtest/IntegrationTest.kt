package se.gustavkarlsson.slackdeadlinereminder.integrationtest

import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

private const val DB_NAME = "deadlines"
private const val DB_USERNAME = "username"
private const val DB_PASSWORD = "password"

@Testcontainers
abstract class IntegrationTest {
    companion object {
        @JvmStatic
        @Container
        protected var postgresqlContainer = PostgreSQLContainer("postgres:12")
            .apply {
                withDatabaseName(DB_NAME)
                withUsername(DB_USERNAME)
                withPassword(DB_PASSWORD)
            }
    }
}
