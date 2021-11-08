package se.gustavkarlsson.slackdeadlinereminder.config

import se.gustavkarlsson.slackdeadlinereminder.models.ApplicationConfig
import se.gustavkarlsson.slackdeadlinereminder.models.DatabaseConfig
import java.nio.file.InvalidPathException
import java.nio.file.Paths
import java.time.DateTimeException
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.time.zone.ZoneRulesException

// FIXME extract interface
object ConfigLoader {
    enum class ConfigKey(val key: String) {
        SLACK_BOT_TOKEN("slack_bot_token"),
        SLACK_SIGNING_SECRET("slack_signing_secret"),
        COMMAND_NAME("command_name"),
        PORT("port"),
        ADDRESS("address"),
        REMINDER_DAYS("reminder_days"),
        REMINDER_TIME("reminder_time"),
        ZONE_ID("zone_id"),
        DB_JSON_FILE("db_json_file"),
        DB_JSON_PRETTY("db_json_pretty"),
        DB_POSTGRES_ADDRESS("db_postgres_address"),
        DB_POSTGRES_USER("db_postgres_user"),
        DB_POSTGRES_PASSWORD("db_postgres_password"),
    }

    fun loadConfig(env: Map<String, String>): Result = try {
        val slackBotToken = env[ConfigKey.SLACK_BOT_TOKEN].validateSlackBotToken()
        val slackSigningSecret = env[ConfigKey.SLACK_SIGNING_SECRET].validateSlackSigningSecret()
        val commandName = env[ConfigKey.COMMAND_NAME, "deadline"].validateCommandName()
        val port = env[ConfigKey.PORT, "8080"].validatePort()
        val address = env[ConfigKey.ADDRESS, "0.0.0.0"].validateAddress()
        val reminderDays = env[ConfigKey.REMINDER_DAYS, "1, 3, 7"].validateReminderDays()
        val reminderTime = env[ConfigKey.REMINDER_TIME, "09:00"].validateReminderTime()
        val zoneId = env[ConfigKey.ZONE_ID, "UTC"].validateZoneId()
        val prettyJson = env[ConfigKey.DB_JSON_PRETTY, "false"].validateBoolean()
        val jsonFileDbConfig = env[ConfigKey.DB_JSON_FILE, ""].validateJsonFileConfig(prettyJson)
        val postgresDbAddress = env[ConfigKey.DB_POSTGRES_ADDRESS, ""].ifBlank { null }
        val postgresDbUser = env[ConfigKey.DB_POSTGRES_USER, ""].ifBlank { null }
        val postgresDbPassword = env[ConfigKey.DB_POSTGRES_PASSWORD, ""].ifBlank { null }

        val databaseConfig = when {
            postgresDbAddress != null && postgresDbUser != null && postgresDbPassword != null ->
                DatabaseConfig.Postgres(postgresDbAddress, postgresDbUser, postgresDbPassword)
            jsonFileDbConfig != null -> jsonFileDbConfig
            else -> DatabaseConfig.InMemory
        }

        val config = ApplicationConfig(
            slackBotToken = slackBotToken,
            slackSigningSecret = slackSigningSecret,
            commandName = commandName,
            port = port,
            address = address,
            reminderDurations = reminderDays,
            reminderTime = reminderTime,
            zoneId = zoneId,
            databaseConfig = databaseConfig,
        )
        Success(config)
    } catch (e: ValidationException) {
        Failure.InvalidConfigurationValue(e.message)
    } catch (e: MissingConfigurationValueException) {
        Failure.MissingConfigurationValue(e.key)
    }

    private operator fun Map<String, String>.get(configKey: ConfigKey, defaultValue: String? = null): String {
        return this[configKey.key] ?: defaultValue ?: missingValue(configKey)
    }

    private fun String.validateSlackBotToken(): String {
        return if (startsWith("xoxb-")) {
            this
        } else {
            failValidation("Invalid slack bot token '$this'")
        }
    }

    private fun String.validateSlackSigningSecret(): String {
        return ifBlank { failValidation("Invalid slack signing secret '$this'") }
    }

    private fun String.validateCommandName(): String {
        return ifBlank { failValidation("Invalid command name '$this'") }
    }

    private fun String.validatePort(): Int {
        val port = toIntOrNull()
        return when {
            port == null -> failValidation("Invalid port '$this'")
            port < 1 -> failValidation("Invalid port '$this'")
            else -> port
        }
    }

    private fun String.validateAddress(): String {
        return ifBlank { failValidation("Invalid address '$this'") }
    }

    private fun String.validateReminderDays(): Set<Duration> {
        if (isBlank()) failValidation("Invalid reminder days '$this'")
        val reminderDays = split(',')
            .map {
                try {
                    it.trim().toLong()
                } catch (t: Throwable) {
                    failValidation("Invalid day '$it'", t)
                }
            }
            .onEach { if (it < 0) failValidation("Reminder days cannot be negative '$it'") }
            .map { Duration.ofDays(it) }
            .toSet()

        return reminderDays + Duration.ZERO
    }

    private fun String.validateReminderTime(): LocalTime {
        return if (isBlank()) {
            failValidation("Reminder time cannot be null or blank")
        } else {
            try {
                LocalTime.parse(this)
            } catch (e: DateTimeParseException) {
                failValidation("Failed to parse reminder time '$this'")
            }
        }
    }

    private fun String.validateZoneId(): ZoneId {
        return if (isBlank()) {
            failValidation("Zone ID cannot be null or blank")
        } else {
            try {
                ZoneId.of(this)
            } catch (e: DateTimeException) {
                failValidation("Invalid Zone ID format", e)
            } catch (e: ZoneRulesException) {
                failValidation("Unknown Zone ID", e)
            }
        }
    }

    private fun String.validateBoolean(): Boolean {
        return when (trim().lowercase()) {
            "true", "yes" -> true
            "false", "no" -> false
            else -> failValidation("Unknown boolean value: '$this'")
        }
    }

    private fun String.validateJsonFileConfig(prettyJson: Boolean): DatabaseConfig.JsonFile? {
        return if (isBlank()) {
            null
        } else {
            val file = try {
                Paths.get(this)
            } catch (e: InvalidPathException) {
                failValidation("Invalid database path", e)
            }
            DatabaseConfig.JsonFile(file, prettyJson)
        }
    }

    private class ValidationException(override val message: String, cause: Throwable?) : Throwable(message, cause)

    private fun failValidation(message: String, cause: Throwable? = null): Nothing {
        throw ValidationException(message, cause)
    }

    private class MissingConfigurationValueException(val key: ConfigKey) : Throwable()

    private fun missingValue(key: ConfigKey): Nothing {
        throw MissingConfigurationValueException(key)
    }

    sealed interface Result
    data class Success(val config: ApplicationConfig) : Result
    sealed interface Failure : Result {
        data class MissingConfigurationValue(val configKey: ConfigKey) : Failure
        data class InvalidConfigurationValue(val message: String) : Failure
    }
}
