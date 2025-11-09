import java.text.SimpleDateFormat
import java.util.*

val mavenGroup = rootProject.property("maven_group").toString()
val modVersion = rootProject.property("mod_version").toString()
val modId = rootProject.property("mod_id").toString()
val minecraftVersion = rootProject.property("minecraft_version").toString()
val forgeVersion = rootProject.property("forge_version").toString()
val kotlinVersion = rootProject.property("kotlin_version").toString()

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("net.minecraftforge.gradle") version "[6.0,6.2)"
}

group = "$mavenGroup.forge"
version = "$modVersion-forge"

repositories {
    maven("https://maven.minecraftforge.net/") // Forge maven
    maven("https://thedarkcolour.github.io/KotlinForForge/") // Kotlin for Forge
    maven("https://maven.shedaniel.me/") // Cloth config
}

dependencies {
    minecraft("net.minecraftforge:forge:$forgeVersion")
    implementation("thedarkcolour:kotlinforforge:4.3.0")
    compileOnly(project(":common"))
}

val Project.minecraft: net.minecraftforge.gradle.common.util.MinecraftExtension
    get() = extensions.getByType()

minecraft.let {
    it.mappings("official", minecraftVersion)
    it.runs {
        create("client") {
            workingDirectory(project.file("run"))
            property("forge.logging.console.level", "debug")
            mods {
                create(modId) {
                    sources(sourceSets.main.get())
                }
            }
        }
    }
}

sourceSets {
    main {
        java {
            srcDir(project(":common").sourceSets.main.get().java)
        }

        kotlin {
            srcDir(project(":common").sourceSets.main.get().kotlin)
        }

        resources {
            srcDir(project(":common").sourceSets.main.get().resources)
        }
    }
}

tasks {
    val javaVersion = JavaVersion.valueOf("VERSION_17")
    compileJava {
        options.encoding = "UTF-8"
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
        if (JavaVersion.current().isJava9Compatible) {
            options.release.set(javaVersion.toString().toInt())
        }
    }

    compileKotlin {
        kotlinOptions {
            jvmTarget = javaVersion.toString()
        }
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(javaVersion.toString()))
        }
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
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
        finalizedBy("reobfJar")
    }
}
