plugins {
    java
}

group = "$modGroup.krylix"
version = coreVersion

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")

    // We will use Gson which is already provided by Minecraft, but for common to compile:
    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    test {
        useJUnitPlatform()
    }
}
