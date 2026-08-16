import java.text.SimpleDateFormat
import java.util.Date

plugins {
    java
    id("fabric-loom") version fabricLoomVersion
}

group = "$modGroup.fabric"
version = "$fabricModVersion-fabric"

repositories {
    maven("https://maven.shedaniel.me/") // Cloth Config
    maven("https://maven.terraformersmc.com/releases/") // Mod Menu
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())

    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    modImplementation("me.shedaniel.cloth:cloth-config-fabric:$clothConfigVersion") {
        exclude(group = "net.fabricmc.fabric-api")
    }

    implementation(project(":common"))
}

sourceSets {
    main {
        java {
            srcDir(project(":common").sourceSets.main.get().java)
        }
        resources {
            srcDir(project(":common").sourceSets.main.get().resources)
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    processResources {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") {
            expand("version" to project.version)
        }
    }

    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        archiveBaseName.set(modId)
        manifest {
            attributes(
                mapOf(
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to project.version,
                    "Implementation-Timestamp" to SimpleDateFormat("yyyy-MM-dd").format(Date()),
                ),
            )
        }
    }

    remapJar {
        archiveBaseName.set(modId)
    }
}
