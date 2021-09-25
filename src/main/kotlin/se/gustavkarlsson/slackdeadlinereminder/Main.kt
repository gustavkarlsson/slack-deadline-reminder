package se.gustavkarlsson.slackdeadlinereminder

import com.slack.api.bolt.App as BoltApp

fun main() {
    // App expects env variables (SLACK_BOT_TOKEN, SLACK_SIGNING_SECRET)
    val boltApp = BoltApp()
    //serveWithKtor(app)
}
