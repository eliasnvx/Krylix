import java.text.SimpleDateFormat
import java.util.Date

plugins {
    java
    id("net.fabricmc.fabric-loom") version fabricLoomVersion
}

group = "$modGroup.fabric"
version = "$fabricModVersion-fabric"

repositories {
    maven("https://maven.shedaniel.me/") // Cloth Config
    maven("https://maven.terraformersmc.com/releases/") // Mod Menu
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")

    implementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    implementation("net.fabricmc.fabric-api:fabric-command-api-v2:3.0.5+e2bdee784c")
    implementation("me.shedaniel.cloth:cloth-config-fabric:$clothConfigVersion")
    compileOnly("com.terraformersmc:modmenu:$modMenuVersion") // optional at runtime: see fabric.mod.json "suggests"

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

// Client GameTests open a real window and are run by hand:
//   KRYLIX_DOCS_SHOTS=1 ./gradlew :fabric:runClientGameTest   (README / mod page screenshots, see tools/docs)
fabricApi {
    configureTests {
        createSourceSet.set(true)
        modId.set("krylix-test")
        enableGameTests.set(false)
        enableClientGameTests.set(true)
        username.set("eliasnvx")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmTarget.toInt()))
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
}
