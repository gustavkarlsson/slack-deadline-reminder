val ktor_version: String by project
val bolt_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    application
    kotlin("jvm") version "1.5.31"
}

group = "se.gustavkarlsson.slack-deadline-reminder"
version = "0.0.1"
application {
    mainClass.set("se.gustavkarlsson.slackdeadlinereminder.MainKt")
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")

    // Logging
    implementation("ch.qos.logback:logback-classic:$logback_version")

    // Slack
    implementation("com.slack.api:bolt:$bolt_version")
    implementation("com.slack.api:bolt-ktor:$bolt_version")

    // Arrow
    implementation("io.arrow-kt:arrow-core:1.0.0")

    // Date NLP
    implementation("com.zoho:hawking:0.1.4")

    // Testing
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlin_version")
}
