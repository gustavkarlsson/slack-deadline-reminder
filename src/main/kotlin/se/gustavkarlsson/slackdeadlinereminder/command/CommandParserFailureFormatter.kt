package se.gustavkarlsson.slackdeadlinereminder.command

// FIXME extract interface
object CommandParserFailureFormatter {
    fun format(result: CommandParser.Failure): String = when (result) {
        CommandParser.Failure.MissingId -> "Missing ID"
        CommandParser.Failure.MissingSubcommand -> "Missing subcommand"
        CommandParser.Failure.MissingSubcommandArgument -> "Missing subcommand argument"
        CommandParser.Failure.MissingTime -> "Missing time"
    }
}
