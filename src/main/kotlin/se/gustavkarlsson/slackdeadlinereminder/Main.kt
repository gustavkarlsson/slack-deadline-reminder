package se.gustavkarlsson.slackdeadlinereminder

import ApplicationConfig
import ConfigLoader
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import se.gustavkarlsson.slackdeadlinereminder.command.CommandParser
import se.gustavkarlsson.slackdeadlinereminder.command.CommandParserFailureFormatter
import se.gustavkarlsson.slackdeadlinereminder.command.CommandResponseFormatter
import se.gustavkarlsson.slackdeadlinereminder.models.ChannelId
import se.gustavkarlsson.slackdeadlinereminder.models.MessageContext
import se.gustavkarlsson.slackdeadlinereminder.models.UserId
import se.gustavkarlsson.slackdeadlinereminder.repo.InMemoryDeadlineRepository
import se.gustavkarlsson.slackdeadlinereminder.runners.BoltRunner
import se.gustavkarlsson.slackdeadlinereminder.runners.CliRunner
import se.gustavkarlsson.slackdeadlinereminder.runners.KtorRunner
import java.time.Clock
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

fun main() {
    val config = when (val result = ConfigLoader.loadConfig(System.getenv())) {
        is ApplicationConfig -> result
        is ConfigLoader.InvalidConfigurationValue -> {
            logger.error { "Configuration error: ${result.message}" }
            exitProcess(1)
        }
        is ConfigLoader.MissingConfigurationValue -> {
            logger.error { "Configuration error: environment variable '${result.configKey.key}' is missing" }
            exitProcess(2)
        }
    }
    val repo = InMemoryDeadlineRepository()
    val notifier = Notifier(repo)
    val app = App(repo, notifier)
    val clock = Clock.systemUTC()
    val commandParser = CommandParser(clock)
    val commandResponseFormatter = CommandResponseFormatter
    val commandParserFailureFormatter = CommandParserFailureFormatter

    val cliRunner: Runner = CliRunner(
        app = app,
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
        commandParser = commandParser,
        commandResponseFormatter = commandResponseFormatter,
        commandParserFailureFormatter = commandParserFailureFormatter,
        config.address,
        config.port,
        config.slackBotToken,
        config.slackSigningSecret,
    )
    val boltRunner: Runner = BoltRunner(
        app = app,
        commandParser = commandParser,
        commandResponseFormatter = commandResponseFormatter,
        commandParserFailureFormatter = commandParserFailureFormatter,
        commandName = config.commandName,
    )
    val runner = cliRunner
    runBlocking {
        runner.run()
    }
}

