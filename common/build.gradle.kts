plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version kotlinVersion
}

group = "$modGroup.common.krylix"
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = jvmTarget
    }
}
