# Krylix - Advanced Kill Feed Mod

<div align="center">

![Krylix Logo](https://img.shields.io/badge/Krylix-Kill%20Feed%20HUD-brightgreen?style=for-the-badge)
![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20.1-orange?style=for-the-badge)
![Forge](https://img.shields.io/badge/Forge-47.1.0-red?style=for-the-badge)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.10-purple?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

**A modern, feature-rich kill feed HUD mod for Minecraft Forge that displays player kills with style, animations, and kill streak tracking.**

</div>

## ✨ Features

### 🎯 Kill Feed Display
- **Horizontal Layout**: Clean, modern kill feed in the top-right corner
- **Player Heads**: Shows actual player skins with correct rendering
- **Mob Heads**: Displays 30+ mob textures with proper UV mapping
- **Weapon Icons**: Animated weapon display with smooth bobbing effect
- **Health Display**: Shows killer's remaining HP
- **Distance Tracking**: Records kill distance for each entry
- **15-Second Display**: Entries fade out smoothly after 15 seconds

### 🔥 Kill Streaks System
Track consecutive kills with epic announcements:
- **DOUBLE KILL!** - 2 kills in 10 seconds
- **TRIPLE KILL!** - 3 kills in 10 seconds
- **MEGA KILL!** - 4 kills in 10 seconds
- **ULTRA KILL!** - 5 kills in 10 seconds
- **MONSTER KILL!** - 6 kills in 10 seconds
- **RAMPAGE!** - 7 kills in 10 seconds
- **GODLIKE!** - 8+ kills in 10 seconds

Each streak comes with:
- 🔊 Unique sound effects with increasing pitch
- 💬 Golden text announcement in chat
- ⏱️ 10-second window to continue the streak

### 🎨 Visual Effects
- **Smooth Animations**: Weapon bobbing animation
- **Fade Out**: Smooth alpha transition before removal
- **No Background**: Clean, minimalist design
- **Dynamic Positioning**: Automatically adjusts for multiple entries
- **Color Coding**: Different colors for killers and victims

### 🎮 Supported Mobs
30+ mob textures including:
- Zombie, Skeleton, Creeper, Spider, Enderman
- Piglin, Zombified Piglin, Wither Skeleton
- Blaze, Ghast, Witch, Phantom
- Guardian, Elder Guardian, Shulker
- Wither, Ender Dragon
- And many more!

## 📋 Requirements

- **Minecraft**: 1.20.1
- **Forge**: 47.1.0 or higher
- **Java**: 17 or higher
- **KotlinForForge**: 4.3.0 (included)

## 🚀 Installation

1. **Download the latest release** from the [Releases page](https://github.com/eliasnvx/Krylix/releases)
2. **Install Forge** 1.20.1-47.1.0 if not already present
3. **Place the JAR file** in your `.minecraft/mods/` directory
4. **Launch Minecraft** with Forge profile

### Quick Setup

```bash
# Download and install
wget https://github.com/eliasnvx/Krylix/releases/latest/download/Krylix-forge.jar
cp Krylix-forge.jar ~/.minecraft/mods/
```

## 🎮 Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/testkill` | `op` | Generate random test kill |
| `/testkill sword` | `op` | Test sword kill |
| `/testkill bow` | `op` | Test bow kill |
| `/testkill axe` | `op` | Test axe kill |
| `/testkill headshot` | `op` | Test headshot kill |
| `/testkill multi` | `op` | Generate multiple test kills |

## 🏗️ Development

### Building from Source

```bash
# Clone the repository
git clone https://github.com/eliasnvx/Krylix.git
cd Krylix

# Build with Gradle
./gradlew :forge:build

# Run client for testing
./gradlew :forge:runClient -Pusername=Player2
```

### Project Structure

```
Krylix/
├── common/                  # Common code (shared)
│   └── src/main/kotlin/
│       └── model/          # Data models
├── forge/                   # Forge implementation
│   └── src/main/kotlin/
│       ├── client/         # Client-side code
│       │   ├── KillFeedHud.kt
│       │   └── ClientEvents.kt
│       └── KrylixServerCommands.kt
├── fabric/                  # Fabric implementation (planned)
└── buildSrc/               # Build configuration
```

### Key Components

#### KillFeedHud.kt
Main HUD rendering logic:
- Player/mob head rendering
- Weapon animation
- Kill streak tracking
- Sound effects

#### KillEntry.kt
Data model for kill events:
- Killer/victim information
- Weapon and distance
- Timestamp for fade-out
- Alpha calculation

#### NetworkPackets.kt
Network synchronization:
- Client-server communication
- Kill event broadcasting

## 🎨 Customization

### Adding New Mobs

Edit `KillFeedHud.kt` to add new mob textures:

```kotlin
private fun getMobTexture(mobName: String?): ResourceLocation? {
    return when (mobName?.lowercase()) {
        "your_mob" -> ResourceLocation("minecraft", "textures/entity/your_mob.png")
        // ...
    }
}
```

### Adjusting Kill Streak Timings

Modify the streak window in `processKillStreak()`:

```kotlin
// Change 10000ms (10 seconds) to your preferred duration
if (currentTime - streak.lastKillTime > 10000) {
    streak.count = 1
}
```

## 🤝 Contributing

We welcome contributions! Please follow these steps:

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/amazing-feature`
3. **Commit your changes**: `git commit -m 'Add amazing feature'`
4. **Push to the branch**: `git push origin feature/amazing-feature`
5. **Open a Pull Request**

### Code Style

- Follow Kotlin coding conventions
- Use meaningful variable names
- Add KDoc comments for public APIs
- Test your changes with `/testkill` commands

## 🐛 Known Issues

- ~~Enderman head rendering~~ ✅ Fixed in v1.0.1
- ~~ConcurrentModificationException crash~~ ✅ Fixed in v1.0.1

## 📈 Roadmap

- [x] **v1.0**: Basic kill feed with player/mob heads
- [x] **v1.1**: Kill streak system
- [x] **v1.2**: Weapon animations
- [ ] **v1.3**: Death type icons (fire, fall, explosion)
- [ ] **v1.4**: Distance-based coloring
- [ ] **v1.5**: Headshot indicator
- [ ] **v2.0**: Configuration GUI
- [ ] **v2.1**: Fabric support

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Credits

- **Lead Developer**: [eliasnvx](https://github.com/eliasnvx)
- **Built with**: [KotlinForForge](https://github.com/thedarkcolour/KotlinForForge)
- **Inspired by**: Classic FPS kill feed systems

## 📊 Statistics

- **30+ Mob Textures**: Comprehensive mob support
- **7 Kill Streak Levels**: From Double Kill to Godlike
- **15-Second Display**: Perfect timing for visibility
- **60 FPS Animations**: Smooth weapon bobbing

---

<div align="center">

**⭐ Star this repository if you find it useful!**

Made with ❤️ for the Minecraft PvP community

</div>
