// Common
const val jvmTarget = "25"

// Minecraft
const val minecraftVersion = "26.1.2"

// Mod
const val modId = "krylix"
const val modGroup = "com.eliasnvx"
const val coreVersion = "1.3.0"

// NeoForge
const val neoForgeVersion = "26.1.2.95" // https://projects.neoforged.net/neoforged/neoforge
const val neoModDevPlugin = "2.0.144" // https://projects.neoforged.net/neoforged/moddevgradle
const val neoForgeModVersion = "$coreVersion-MC$minecraftVersion"
const val neoForgeModArchive = "$modId-$neoForgeModVersion-neoforge"

// Fabric
const val fabricLoomVersion = "1.17.12"
const val fabricLoaderVersion = "0.19.3"
const val fabricApiVersion = "0.155.2+26.1.2"
const val fabricModVersion = "$coreVersion-MC$minecraftVersion"
const val fabricModArchive = "$modId-$fabricModVersion-fabric"

// Forge
const val forgeGradleVersion = "6.0.54"
const val forgeVersion = "26.1.2"
const val forgeModVersion = "$coreVersion-MC$minecraftVersion"
const val forgeModArchive = "$modId-$forgeModVersion-forge"

// External dependencies
const val clothConfigVersion = "26.1.154" // https://linkie.shedaniel.dev/dependencies
