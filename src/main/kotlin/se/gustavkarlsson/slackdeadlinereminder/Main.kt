package se.gustavkarlsson.slackdeadlinereminder

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import se.gustavkarlsson.slackdeadlinereminder.command.CommandParser
import se.gustavkarlsson.slackdeadlinereminder.command.CommandParserFailureFormatter
import se.gustavkarlsson.slackdeadlinereminder.command.CommandResponseFormatter
import se.gustavkarlsson.slackdeadlinereminder.config.ConfigLoader
import se.gustavkarlsson.slackdeadlinereminder.models.ChannelId
import se.gustavkarlsson.slackdeadlinereminder.models.DatabaseConfig
import se.gustavkarlsson.slackdeadlinereminder.models.MessageContext
import se.gustavkarlsson.slackdeadlinereminder.models.UserId
import se.gustavkarlsson.slackdeadlinereminder.nlp.HawkingNlpDateParser
import se.gustavkarlsson.slackdeadlinereminder.repo.ExposedDbRepository
import se.gustavkarlsson.slackdeadlinereminder.repo.InMemoryDeadlineRepository
import se.gustavkarlsson.slackdeadlinereminder.repo.JsonFileRepository
import se.gustavkarlsson.slackdeadlinereminder.runners.CliRunner
import se.gustavkarlsson.slackdeadlinereminder.runners.KtorRunner
import java.time.Clock
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

fun main() {
    val config = when (val result = ConfigLoader.loadConfig(System.getenv())) {
        is ConfigLoader.Success -> result.config
        is ConfigLoader.Failure.InvalidConfigurationValue -> {
            logger.error { "Configuration error: ${result.message}" }
            exitProcess(1)
        }
        is ConfigLoader.Failure.MissingConfigurationValue -> {
            logger.error { "Configuration error: environment variable '${result.configKey.key}' is missing" }
            exitProcess(2)
        }
    }
    val repository = when (val databaseConfig = config.databaseConfig) {
        DatabaseConfig.InMemory -> InMemoryDeadlineRepository()
        is DatabaseConfig.JsonFile -> JsonFileRepository(databaseConfig.file, databaseConfig.prettyPrint)
        is DatabaseConfig.Postgres -> ExposedDbRepository(databaseConfig)
    }
    val notifier = Notifier(repository, config.reminderTime, config.reminderDurations)
    val app = CommandProcessor(repository)
    val clock = Clock.system(config.zoneId)
    val nlpDateParser = HawkingNlpDateParser(clock)
    val commandParser = CommandParser(nlpDateParser)
    val commandResponseFormatter = CommandResponseFormatter
    val commandParserFailureFormatter = CommandParserFailureFormatter

    val cliRunner: Runner = CliRunner(
        app = app,
        notifier = notifier,
        commandParser = commandParser,
        commandResponseFormatter = commandResponseFormatter,
        commandParserFailureFormatter = commandParserFailureFormatter,
        commandName = config.commandName,
        messageContext = MessageContext(
            userId = UserId("usr_guk"),
            channelId = ChannelId("chn_deadlines"),
        ),
    )
    val ktorRunner: Runner = KtorRunner(
        app = app,
        notifier = notifier,
        commandParser = commandParser,
        commandResponseFormatter = commandResponseFormatter,
        commandParserFailureFormatter = commandParserFailureFormatter,
        address = config.address,
        port = config.port,
        slackBotToken = config.slackBotToken,
        slackSigningSecret = config.slackSigningSecret,
    )
    val runner = cliRunner
    runBlocking {
        runner.run()
    }
}

