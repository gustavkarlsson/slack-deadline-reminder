import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeParseException

object ConfigLoader {
    enum class ConfigKey(val key: String) {
        SLACK_BOT_TOKEN("slack_bot_token"),
        SLACK_SIGNING_SECRET("slack_signing_secret"),
        COMMAND_NAME("command_name"),
        PORT("port"),
        ADDRESS("address"),
        REMINDER_DAYS("reminder_days"),
        REMINDER_TIME("reminder_time"),
    }

    fun loadConfig(env: Map<String, String>): Result = try {
        val slackBotToken = env[ConfigKey.SLACK_BOT_TOKEN].validateSlackBotToken()
        val slackSigningSecret = env[ConfigKey.SLACK_SIGNING_SECRET].validateSlackSigningSecret()
        val commandName = env[ConfigKey.COMMAND_NAME, "deadline"].validateCommandName()
        val port = env[ConfigKey.PORT, "8080"].validatePort()
        val address = env[ConfigKey.ADDRESS, "0.0.0.0"].validateAddress()
        val reminderDays = env[ConfigKey.REMINDER_DAYS, "1, 3, 7"].validateReminderDays()
        val reminderTime = env[ConfigKey.REMINDER_TIME, "09:00"].validateReminderTime()

        ApplicationConfig(slackBotToken, slackSigningSecret, commandName, port, address, reminderDays, reminderTime)
    } catch (e: ValidationException) {
        InvalidConfigurationValue(e.message)
    } catch (e: MissingConfigurationValueException) {
        MissingConfigurationValue(e.key)
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
        if (isNullOrBlank()) failValidation("Invalid reminder days '$this'")
        val reminderDays = split(',')
            .map {
                try {
                    it.toLong()
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
        return if (isNullOrBlank()) {
            failValidation("Reminder time cannot be null or blank")
        } else {
            try {
                LocalTime.parse(this)
            } catch (e: DateTimeParseException) {
                failValidation("Failed to parse reminder time '$this'")
            }
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
    data class MissingConfigurationValue(val configKey: ConfigKey) : Result
    data class InvalidConfigurationValue(val message: String) : Result
}


data class ApplicationConfig(
    val slackBotToken: String,
    val slackSigningSecret: String,
    val commandName: String,
    val port: Int,
    val address: String,
    val reminderDurations: Set<Duration>,
    val reminderTime: LocalTime,
) : ConfigLoader.Result
