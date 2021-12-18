import org.gradle.api.tasks.testing.logging.TestLogEvent.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version: String by project
val bolt_version: String by project
val kotlin_version: String by project
val coroutines_version: String by project
val logback_version: String by project
val kotlinx_serialization_version: String by project
val exposed_version: String by project
val postgres_version: String by project

plugins {
    application
    kotlin("jvm") version "1.6.0"
    kotlin("plugin.serialization") version "1.6.0"
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
    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:$coroutines_version"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

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

    // Database
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposed_version")
    implementation("org.postgresql:postgresql:$postgres_version")

    // Testing
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlin_version")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("org.testcontainers:postgresql:1.16.2")
    testImplementation("org.testcontainers:junit-jupiter:1.16.2")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events = setOf(PASSED, SKIPPED, FAILED)
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}
