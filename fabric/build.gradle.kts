plugins {
    id("architectury-plugin")
    id("dev.architectury.loom")
}

val minecraftVersion = rootProject.property("minecraft_version").toString()
val fabricLoaderVersion = rootProject.property("fabric_loader_version").toString()
val fabricApiVersion = rootProject.property("fabric_api_version").toString()
val fabricKotlinVersion = rootProject.property("fabric_kotlin_version").toString()
val architecturyVersion = rootProject.property("architectury_version").toString()

architectury {
    platformSetupLoomIde()
    fabric()
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())

    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    modApi("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    modApi("dev.architectury:architectury-fabric:$architecturyVersion")

    // Fabric Language Kotlin
    modImplementation("net.fabricmc:fabric-language-kotlin:$fabricKotlinVersion")

    // Common
    implementation(project(":common"))
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}
