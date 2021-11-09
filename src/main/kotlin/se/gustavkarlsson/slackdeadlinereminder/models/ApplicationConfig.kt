package se.gustavkarlsson.slackdeadlinereminder.models

import java.time.LocalTime
import java.time.ZoneId

data class ApplicationConfig(
    val slackBotToken: String,
    val slackSigningSecret: String,
    val commandName: String,
    val port: Int,
    val address: String,
    val reminderDays: Set<Int>,
    val reminderTime: LocalTime,
    val zoneId: ZoneId,
    val databaseConfig: DatabaseConfig,
)
