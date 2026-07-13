rootProject.name = "krylix"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.minecraftforge.net/") {
            name = "Forge"
        }
    }
}

include("common")
include("forge")
