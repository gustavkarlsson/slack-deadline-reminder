package se.gustavkarlsson.slackdeadlinereminder

import kotlinx.coroutines.runBlocking
import se.gustavkarlsson.slackdeadlinereminder.command.CommandParser
import se.gustavkarlsson.slackdeadlinereminder.command.CommandParserFailureFormatter
import se.gustavkarlsson.slackdeadlinereminder.command.CommandResponseFormatter
import se.gustavkarlsson.slackdeadlinereminder.repo.InMemoryDeadlineRepository
import se.gustavkarlsson.slackdeadlinereminder.runners.BoltRunner
import se.gustavkarlsson.slackdeadlinereminder.runners.CliRunner
import se.gustavkarlsson.slackdeadlinereminder.runners.KtorRunner
import java.time.Clock

fun main() {
    val repo = InMemoryDeadlineRepository()
    val notifier = Notifier(repo)
    val app = App(repo, notifier)
    val clock = Clock.systemUTC()
    val commandParser = CommandParser(clock)
    val commandResponseFormatter = CommandResponseFormatter
    val commandParserFailureFormatter = CommandParserFailureFormatter

    val commandName = "deadline"
    val cliRunner: Runner = CliRunner(
        app = app,
        commandParser = commandParser,
        commandResponseFormatter = commandResponseFormatter,
        commandParserFailureFormatter = commandParserFailureFormatter,
        commandName = commandName,
        userName = "gustav",
        channelName = "deadlines",
    )
    val ktorRunner: Runner = KtorRunner(
        app = app,
        commandParser = commandParser,
        commandResponseFormatter = commandResponseFormatter,
        commandParserFailureFormatter = commandParserFailureFormatter
    )
    val boltRunner: Runner = BoltRunner(
        app = app,
        commandParser = commandParser,
        commandResponseFormatter = commandResponseFormatter,
        commandParserFailureFormatter = commandParserFailureFormatter,
        commandName = commandName,
    )
    val runner = cliRunner
    runBlocking {
        runner.run()
    }
}

