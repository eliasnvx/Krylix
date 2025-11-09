plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    id("architectury-plugin") version "3.4.162"
    id("dev.architectury.loom") version "1.7.+" apply false
}

architectury {
    minecraft = project.property("minecraft_version").toString()
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "kotlinx-serialization")

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = listOf("-Xjvm-default=all")
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

allprojects {
    group = project.property("maven_group").toString()
    version = project.property("mod_version").toString()

    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev/")
        maven("https://maven.minecraftforge.net/")
    }
}
