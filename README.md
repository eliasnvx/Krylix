# Krylix - Advanced Kill Feed & PvP Stats Mod

<div align="center">

![Krylix Logo](https://img.shields.io/badge/Krylix-Kill%20Feed%20HUD-brightgreen?style=for-the-badge)
![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.1-orange?style=for-the-badge)
![Loaders](https://img.shields.io/badge/Loader-NeoForge%20|%20Fabric-red?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-21-purple?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

[![CurseForge](https://img.shields.io/badge/CurseForge-Download-orange?style=for-the-badge&logo=curseforge)](https://curseforge.com/minecraft/mc-mods/krylix)
[![Modrinth](https://img.shields.io/badge/Modrinth-Download-green?style=for-the-badge&logo=modrinth)](https://modrinth.com/mod/krylix)
[![GitHub](https://img.shields.io/badge/GitHub-Source-black?style=for-the-badge&logo=github)](https://github.com/eliasnvx/Krylix)

**A modern kill feed, mob-kill tracker, crosshair health indicator, and player leaderboard for Minecraft (NeoForge & Fabric) — with a real GUI, persistent settings, and full localization.**

</div>

## ✨ Features

### 🎯 Kill Feed
- Horizontal entries in the top corner with player/mob heads, weapon icon, and a heart + HP number
- 30+ mob face textures verified against decompiled vanilla models (not guessed UVs)
- Clean self-kill / environmental death entries (no confusing blank killer avatar)
- Rounded-corner avatars, cached skins (no per-frame skin lookups)

### 💀 Death Recap
- An elegant "Death Recap" overlay displayed directly on the death screen.
- Shows exactly who killed you, with what weapon, from what distance, their remaining HP, and the total damage you dealt to them.

### 🧟 Mob Kill Stats Panel
- Top-left HUD panel tracking hostile-mob kills for the current world (per-world, not per-dimension)
- Persists across restarts via server-side saved data

### ❤️ Health Indicator
- Compact HP readout above whatever entity is under your crosshair, extended-range raytrace (works past interaction distance)
- Smooth 3D billboarding above mob heads, mirroring vanilla nametags perfectly.

### 🏆 Leaderboard (GUI, not chat)
- Dedicated screen (`O` by default) styled as a dark rounded panel, not a chat dump
- **PvP tab**: kills / deaths / K-D per player
- **Mob Kills tab**: players ranked by personal mob-kill count
- Self-row highlight, avatars, empty states, and page-switch controls that appear once you scroll to the bottom of a page (60 entries/page)

### ⚙️ Configuration (Cloth Config)
All settings are in the mod's config screen and persist across restarts:
- Kill feed display duration, max visible entries
- Toggle kill feed / mob stats panel / health indicator independently
- **Server-side**: restrict kill broadcasts to players in the same dimension (default: off, broadcasts everywhere)

### ⌨️ Keybinds (bound by default, no setup needed)
| Key | Action |
|-----|--------|
| `K` | Toggle kill feed |
| `L` | Toggle mob stats panel |
| `O` | Open the leaderboard |
| `H` | Toggle health indicator |

Toggling a HUD element plays a click sound and shows an action-bar confirmation (ON/OFF), so it's never unclear whether the key press registered.

### 🌍 Localization
Fully translated into 13 languages:

English · Русский · Беларуская · Українська · Polski · 简体中文 · 繁體中文（台灣）· Deutsch · Svenska · Nederlands · Español · Português (Brasil) · Français

Switch your Minecraft language and every config option, keybind name, leaderboard label, and toggle message updates — no reload required.

### 🎮 Supported Mob Textures
27 mobs with textures verified via decompiled vanilla models and cropped-texture visual checks (not guesswork):

Zombie, Husk, Drowned, Zombie Villager, Skeleton, Wither Skeleton, Stray, Creeper, Spider, Cave Spider, Piglin, Piglin Brute, Zombified Piglin, Blaze, Ghast, Witch, Pillager, Vindicator, Evoker, Silverfish, Endermite, Iron Golem, Snow Golem, Enderman, Ravager, Warden, Wither

Mobs with composite geometry where no single crop reads as a recognizable face intentionally fall back to a neutral colored square instead (e.g. Slime, Magma Cube, Guardian, Shulker, Phantom, Ender Dragon).

## 📋 Requirements

- **Minecraft**: 1.21.1
- **NeoForge**: 21.1.77 or higher
- **Fabric**: Fabric API + Fabric Loader
- **Java**: 21

## 🚀 Installation

1. **Download the latest release** from the [Releases page](https://github.com/eliasnvx/Krylix/releases) or [Modrinth](https://modrinth.com/mod/krylix)
2. **Install NeoForge or Fabric** for Minecraft 1.21.1
3. **Place the JAR file** in your `.minecraft/mods/` directory
4. **Launch Minecraft**

## 🎮 Commands

| Command | Side | Permission | Description |
|---------|------|------------|-------------|
| `/krylix toggle` | Server | `op` | Enable/disable the kill feed server-wide |
| `/krylix status` | Server | `op` | Show server-side kill feed status |
| `/krylix hud toggle` | Client | — | Toggle the kill feed HUD (same as pressing `K`) |
| `/krylix hud clear` | Client | — | Clear active kill feed entries |
| `/krylix hud count` | Client | — | Show active notification count |
| `/krylix leaderboard testfill [count]` | Client | — | Fill the leaderboard with random test players (visual only, not saved) |
| `/krylix leaderboard clear` | Client | — | Clear the test leaderboard data |

## 🏗️ Development

### Building from Source

```bash
# Clone the repository
git clone https://github.com/eliasnvx/Krylix.git
cd Krylix

# Build all modules (NeoForge and Fabric)
./gradlew build

# Run NeoForge client for testing
./gradlew :neoforge:runClient

# Run Fabric client for testing
./gradlew :fabric:runClient
```

### Project Structure (Multi-Loader Architecture)

```
Krylix/
├── buildSrc/                 # Shared Gradle version constants
├── common/                   # Shared Java 21 core logic, data models, translations, textures
│   └── src/main/java/com/eliasnvx/krylix/
│       ├── core/             # Config options, Enums
│       └── model/            # KillEntry
├── neoforge/                 # NeoForge 1.21.1 implementation
│   └── src/main/java/com/eliasnvx/krylix/forge/
│       ├── client/           # HUD rendering, Leaderboard GUI, Keybinds
│       ├── config/           # Cloth Config wiring
│       └── network/          # NeoForge Payload Registry
└── fabric/                   # Fabric 1.21.1 implementation
    └── src/main/java/com/eliasnvx/krylix/fabric/
        ├── client/           # HUD rendering, Keybinds (Fabric API)
        ├── config/           # Cloth Config wiring
        └── network/          # Fabric ClientPlayNetworking
```

### Key Components

- **`KillFeedHud.java`** — kill feed rendering: heads, weapon icon, heart/HP
- **`MobStatsHud.java`** / **`MobStatsClient.java`** — top-left aggregate mob-kill panel
- **`LeaderboardScreen.java`** — the GUI leaderboard with pagination and sorting
- **`DeathRecapClient.java`** — death screen overlay
- **`HealthIndicator.java`** — 3D billboarding nametags for mob health over crosshair
- **`HudRender.java`** — shared scissor-based rounded-corner rendering helper
- **`MobTextures.java`** — the verified mob-face UV atlas

## 🤝 Contributing

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/amazing-feature`
3. **Commit your changes**: `git commit -m 'Add amazing feature'`
4. **Push to the branch**: `git push origin feature/amazing-feature`
5. **Open a Pull Request**

### Code Style
- Pure Java 21.
- Logic is shared via `:common` as much as possible.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Credits

- **Lead Developer**: [eliasnvx](https://github.com/eliasnvx)
- **Built with**: [Cloth Config](https://github.com/shedaniel/cloth-config)

---

<div align="center">

**⭐ Star this repository if you find it useful!**

Made with ❤️ for the Minecraft PvP community

</div>
