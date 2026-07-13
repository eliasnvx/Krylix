# Krylix - Advanced Kill Feed & PvP Stats Mod

<div align="center">

![Krylix Logo](https://img.shields.io/badge/Krylix-Kill%20Feed%20HUD-brightgreen?style=for-the-badge)
![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20.1-orange?style=for-the-badge)
![Forge](https://img.shields.io/badge/Forge-47.1.0-red?style=for-the-badge)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.10-purple?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

[![CurseForge](https://img.shields.io/badge/CurseForge-Download-orange?style=for-the-badge&logo=curseforge)](https://curseforge.com/minecraft/mc-mods/krylix)
[![Modrinth](https://img.shields.io/badge/Modrinth-Download-green?style=for-the-badge&logo=modrinth)](https://modrinth.com/mod/krylix)
[![GitHub](https://img.shields.io/badge/GitHub-Source-black?style=for-the-badge&logo=github)](https://github.com/eliasnvx/Krylix)

**A modern kill feed, mob-kill tracker, crosshair health indicator, and player leaderboard for Minecraft Forge — with a real GUI, persistent settings, and full localization.**

</div>

## ✨ Features

### 🎯 Kill Feed
- Horizontal entries in the top corner with player/mob heads, weapon icon, and a heart + HP number
- 30+ mob face textures verified against decompiled vanilla models (not guessed UVs)
- Clean self-kill / environmental death entries (no confusing blank killer avatar)
- Rounded-corner avatars, cached skins (no per-frame skin lookups)

### 🧟 Mob Kill Stats Panel
- Top-left HUD panel tracking hostile-mob kills for the current world (per-world, not per-dimension)
- Persists across restarts via server-side saved data

### ❤️ Health Indicator
- Compact HP readout above whatever entity is under your crosshair, extended-range raytrace (works past interaction distance)
- 4 configurable bar styles: Blocks, ASCII, Dots, Number Only

### 🏆 Leaderboard (GUI, not chat)
- Dedicated screen (`O` by default) styled as a dark rounded panel, not a chat dump
- **PvP tab**: kills / deaths / K-D per player
- **Mob Kills tab**: players ranked by personal mob-kill count
- Self-row highlight, avatars, empty states, and page-switch controls that appear once you scroll to the bottom of a page (60 entries/page)

### ⚙️ Configuration (Cloth Config)
All settings are in the mod's config screen and persist across restarts:
- Kill feed display duration, max visible entries
- Toggle kill feed / mob stats panel / health indicator independently
- Health bar style
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

Mobs with composite geometry where no single crop reads as a recognizable face — confirmed by decompiling their models and visually inspecting the actual crop, not guessed — intentionally fall back to a neutral colored square instead: Slime, Magma Cube (eyes/mouth are separate small cubes, not part of a single face rectangle), Guardian, Elder Guardian, Shulker (front-face crop is just plain scale/shell pattern with no recognizable feature), Phantom (head is a 7:3 flat rectangle that distorts badly stretched into a square icon), Vex, Hoglin, Zoglin, Ender Dragon (no single head cube in its model).

## 📋 Requirements

- **Minecraft**: 1.20.1
- **Forge**: 47.1.0 or higher
- **Java**: 17 or higher
- **KotlinForForge**: 4.3.0 (included)

Forge only — there is no Fabric build.

## 🚀 Installation

1. **Download the latest release** from the [Releases page](https://github.com/eliasnvx/Krylix/releases) or [Modrinth](https://modrinth.com/mod/krylix)
2. **Install Forge** 1.20.1-47.1.0 if not already present
3. **Place the JAR file** in your `.minecraft/mods/` directory
4. **Launch Minecraft** with the Forge profile

## 🎮 Commands

| Command | Side | Permission | Description |
|---------|------|------------|-------------|
| `/krylix toggle` | Server | `op` | Enable/disable the kill feed server-wide |
| `/krylix status` | Server | `op` | Show server-side kill feed status |
| `/krylix hud toggle` | Client | — | Toggle the kill feed HUD (same as pressing `K`) |
| `/krylix hud clear` | Client | — | Clear active kill feed entries |
| `/krylix hud count` | Client | — | Show active notification count |
| `/krylix leaderboard testfill [count]` | Client | — | Fill the leaderboard with random test players (1–500, default 60) to preview scrolling/pagination — visual only, not saved |
| `/krylix leaderboard clear` | Client | — | Clear the test leaderboard data |

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
├── buildSrc/                 # Shared Gradle version constants (Versions.kt)
├── common/                   # Platform-agnostic code
│   └── src/main/kotlin/com/eliasnvx/krylix/
│       ├── core/             # Config data model, HealthBarStyle
│       └── model/            # KillEntry
└── forge/                    # Forge implementation (all real logic lives here)
    └── src/main/kotlin/com/eliasnvx/krylix/forge/
        ├── client/            # HUD rendering, leaderboard GUI, keybinds, textures
        ├── config/            # Cloth Config wiring
        ├── network/           # SimpleChannel packets
        ├── KrylixForge.kt     # Mod entry point, death-event handling
        ├── KillFeedManager.kt
        ├── MobKillStatsData.kt      # Per-world aggregate mob kills (SavedData)
        └── PlayerKillStatsData.kt   # Per-world per-player kills/deaths/mob-kills (SavedData)
```

### Key Components

- **`KillFeedHud.kt`** — kill feed rendering: heads, weapon icon, heart/HP
- **`MobStatsHud.kt`** / **`MobStatsClient.kt`** — top-left aggregate mob-kill panel
- **`LeaderboardScreen.kt`** — the GUI leaderboard (custom-drawn, not `ObjectSelectionList`)
- **`HealthIndicator.kt`** — crosshair-target world-space HP billboard, mirrors vanilla nametag rendering
- **`HudRender.kt`** — shared scissor-based rounded-corner rendering helper
- **`MobTextures.kt`** — the verified mob-face UV atlas
- **`NetworkPackets.kt`** — `SimpleChannel` packets for kill notifications and stat sync

## 🧪 Testing

The `common` module has JUnit 5 unit tests for `KillEntry` (fade-out expiry/alpha, self-kill/suicide detection):

```bash
./gradlew :common:test
```

## 🤝 Contributing

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/amazing-feature`
3. **Commit your changes**: `git commit -m 'Add amazing feature'`
4. **Push to the branch**: `git push origin feature/amazing-feature`
5. **Open a Pull Request**

### Code Style

- Kotlin, enforced via `ktlint` (`./gradlew :forge:build` runs the check)
- No unverified UV/texture guesses — decompile and visually confirm before adding a new mob texture

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Credits

- **Lead Developer**: [eliasnvx](https://github.com/eliasnvx)
- **Built with**: [KotlinForForge](https://github.com/thedarkcolour/KotlinForForge), [Cloth Config](https://github.com/shedaniel/cloth-config)

---

<div align="center">

**⭐ Star this repository if you find it useful!**

Made with ❤️ for the Minecraft PvP community

</div>
