package se.gustavkarlsson.slackdeadlinereminder

import kotlinx.coroutines.runBlocking
import se.gustavkarlsson.slackdeadlinereminder.app.App
import se.gustavkarlsson.slackdeadlinereminder.cli.CliApp
import se.gustavkarlsson.slackdeadlinereminder.command.CommandParser
import se.gustavkarlsson.slackdeadlinereminder.repo.InMemoryDeadlineRepository
import java.time.Clock

fun main() {
    val repo = InMemoryDeadlineRepository()
    val app = App(repo)
    val clock = Clock.systemUTC()
    val commandParser = CommandParser(clock)
    val cliApp = CliApp(app, commandParser, "deadline", "gustav")
    runBlocking {
        cliApp.run()
    }
}
