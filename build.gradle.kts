val ktor_version: String by project
val bolt_version: String by project
val kotlin_version: String by project
val coroutines_version: String by project
val logback_version: String by project
val kotlinx_serialization_version: String by project

plugins {
    application
    kotlin("jvm") version "1.5.31"
    kotlin("plugin.serialization") version "1.5.31"
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
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")

    // Ktor
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")

    // Logging
    implementation("io.github.microutils:kotlin-logging:2.0.11")
    runtimeOnly("ch.qos.logback:logback-classic:$logback_version")

    // Slack
    implementation("com.slack.api:bolt:$bolt_version")
    implementation("com.slack.api:bolt-jetty:$bolt_version")
    implementation("com.slack.api:bolt-ktor:$bolt_version")

    // Date NLP
    implementation("com.zoho:hawking:0.1.4")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinx_serialization_version")

    // Testing
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlin_version")
}
