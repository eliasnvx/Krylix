import java.text.SimpleDateFormat
import java.util.Date

plugins {
    java
    id("net.neoforged.moddev") version neoModDevPlugin
}

group = "$modGroup.neoforge"
version = "$neoForgeModVersion-neoforge"

repositories {
    maven("https://maven.shedaniel.me/") // Cloth config
}

neoForge {
    version = neoForgeVersion

    runs {
        create("client") {
            client()
            systemProperty("neoforge.logging.console.level", "debug")
            systemProperty("neoforge.earlydisplay", "false")
            systemProperty("fml.earlyprogresswindow", "false")
            if (project.hasProperty("username")) {
                programArguments.addAll("--username", project.property("username").toString())
            }
        }
        create("server") {
            server()
            systemProperty("neoforge.logging.console.level", "debug")
            programArgument("--nogui")
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    implementation("me.shedaniel.cloth:cloth-config-neoforge:$clothConfigVersion")
    compileOnly(project(":common"))
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
