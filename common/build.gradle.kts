plugins {
    id("architectury-plugin")
    id("dev.architectury.loom")
}

val minecraftVersion = rootProject.property("minecraft_version").toString()
val kotlinVersion = rootProject.property("kotlin_version").toString()
val kotlinxSerializationVersion = rootProject.property("kotlinx_serialization_version").toString()
val kotlinxCoroutinesVersion = rootProject.property("kotlinx_coroutines_version").toString()
val architecturyVersion = rootProject.property("architectury_version").toString()

architectury {
    common(rootProject.property("enabled_platforms").toString().split(","))
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())

    // Architectury
    modImplementation("dev.architectury:architectury:$architecturyVersion")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
}
