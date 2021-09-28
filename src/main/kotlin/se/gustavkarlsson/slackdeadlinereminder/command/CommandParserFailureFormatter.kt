package se.gustavkarlsson.slackdeadlinereminder.command

object CommandParserFailureFormatter {
    fun format(result: CommandParser.Result.Failure): String = when (result) {
        CommandParser.Result.Failure.MissingId -> "Missing ID"
        CommandParser.Result.Failure.MissingSubcommand -> "Missing subcommand"
        CommandParser.Result.Failure.MissingSubcommandArgument -> "Missing subcommand argument"
        CommandParser.Result.Failure.MissingTime -> "Missing time"
    }
}
