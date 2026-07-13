plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version kotlinVersion
}

group = "$modGroup.krylix"
version = coreVersion

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Logging
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = jvmTarget
    }
    test {
        useJUnitPlatform()
    }
}
