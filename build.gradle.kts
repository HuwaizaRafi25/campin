plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    application
}

group = "id.ac.polman.oop.campin"
version = "1.0.0"

repositories {
    mavenCentral()
}

val ktorVersion = "2.3.12"

dependencies {
    // Database Driver Utama: MySQL 8.0 + Fallback SQLite agar test lokal selalu jalan
    implementation("com.mysql:mysql-connector-j:8.4.0")
    implementation("org.xerial:sqlite-jdbc:3.46.0.0")

    // Ktor Client untuk Konsumsi REST API Publik (Open-Meteo & Midtrans)
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // Ktor Server untuk Mobile-First Android Web UI & REST Endpoint
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("org.slf4j:slf4j-nop:2.0.13")

    // Unit Testing JUnit 5
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("MainKt")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

