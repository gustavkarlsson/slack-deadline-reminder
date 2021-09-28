package se.gustavkarlsson.slackdeadlinereminder

import kotlinx.coroutines.runBlocking
import se.gustavkarlsson.slackdeadlinereminder.app.App
import se.gustavkarlsson.slackdeadlinereminder.cli.CliApp
import se.gustavkarlsson.slackdeadlinereminder.command.CommandParser
import se.gustavkarlsson.slackdeadlinereminder.command.CommandParserFailureFormatter
import se.gustavkarlsson.slackdeadlinereminder.command.CommandResponseFormatter
import se.gustavkarlsson.slackdeadlinereminder.repo.InMemoryDeadlineRepository
import java.time.Clock

fun main() {
    val repo = InMemoryDeadlineRepository()
    val notifier = Notifier(repo)
    val app = App(repo, notifier)
    val clock = Clock.systemUTC()
    val commandParser = CommandParser(clock)
    val commandName = "deadline"
    val cliApp = CliApp(
        app = app,
        commandParser = commandParser,
        commandResponseFormatter = CommandResponseFormatter,
        commandParserFailureFormatter = CommandParserFailureFormatter(commandName),
        commandName = commandName,
        userName = "gustav",
        channelName = "deadlines",
    )
    runBlocking {
        cliApp.run()
    }
}
