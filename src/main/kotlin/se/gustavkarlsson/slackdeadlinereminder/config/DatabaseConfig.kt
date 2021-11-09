package se.gustavkarlsson.slackdeadlinereminder.config

import java.nio.file.Path

sealed interface DatabaseConfig {
    object InMemory : DatabaseConfig

    data class JsonFile(
        val file: Path,
        val prettyPrint: Boolean,
    ) : DatabaseConfig

    data class Postgres(
        val address: String,
        val userName: String,
        val password: String,
    ) : DatabaseConfig
}
