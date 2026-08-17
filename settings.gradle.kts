rootProject.name = "krylix"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.neoforged.net/releases") {
            name = "NeoForged"
        }
        maven("https://maven.fabricmc.net/") {
            name = "FabricMC"
        }
        maven("https://maven.minecraftforge.net/") {
            name = "Forge"
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

include("common")
include("neoforge")
include("fabric")
// include("forge")
