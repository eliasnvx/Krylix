allprojects {
    repositories {
        maven("https://libraries.minecraft.net/") {
            name = "Minecraft"
        }
        maven("https://maven.minecraftforge.net/") {
            name = "Forge"
        }
        maven("https://maven.shedaniel.me/") {
            name = "Shedaniel"
        }
        mavenCentral()
    }
}
