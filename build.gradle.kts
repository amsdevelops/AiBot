plugins {
    kotlin("plugin.serialization") version "2.2.10"
    `application`
    kotlin("jvm") version "2.2.10"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "ams.dev"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("ai.bot.app.Application")
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(22)
}

tasks.test {
    useJUnitPlatform()
}

sourceSets {
    main {
        resources {
            srcDirs("src/main/resources")
        }
    }
}

tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

dependencies {
    testImplementation(kotlin("test"))
    // Telegram Bot
    implementation(libs.telegram.bots)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlin.coroutines)
    implementation(libs.ktor.client.cio)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.modelcontextprotocol.kotlin.sdk)
    implementation(libs.retrofit.kotlinx.serialization.converter)


    implementation(libs.ktormCore)
    implementation(libs.ktormSupport)
    implementation(libs.mySqlConnector)
}