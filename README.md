# Krylix - Advanced Kill Feed, Mob Stats & PvP Suite

<div align="center">

![Krylix Logo](https://img.shields.io/badge/Krylix-Kill%20Feed%20HUD-brightgreen?style=for-the-badge)
![Minecraft Version](https://img.shields.io/badge/Minecraft-26.1.2-orange?style=for-the-badge)
![Loaders](https://img.shields.io/badge/Loader-NeoForge%20|%20Fabric-red?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-25-purple?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

[![CurseForge](https://img.shields.io/badge/CurseForge-Download-orange?style=for-the-badge&logo=curseforge)](https://curseforge.com/minecraft/mc-mods/krylix)
[![Modrinth](https://img.shields.io/badge/Modrinth-Download-green?style=for-the-badge&logo=modrinth)](https://modrinth.com/mod/krylix)
[![GitHub](https://img.shields.io/badge/GitHub-Source-black?style=for-the-badge&logo=github)](https://github.com/eliasnvx/Krylix)

**A state-of-the-art kill feed, mob-kill tracker, crosshair health indicator with genuine Minecraft heart textures, and player leaderboard for Minecraft (NeoForge & Fabric) — featuring smooth modern GUI rendering, persistent settings, and full localization.**

</div>

## ✨ Features

### 🎯 Kill Feed
- Horizontal notifications in the top corner with player/mob heads, weapon icon, and animated HP indicator.
- 36+ mob face textures verified against decompiled vanilla model geometries with pixel-perfect UV mapping.
- Clean self-kill and environmental death entries.
- Native player face rendering with support for 3D outer layers (hats, hair, accessories).

### 💀 Death Recap
- An elegant "Death Recap" overlay displayed directly on the death screen.
- Shows who killed you, weapon used, combat distance, killer's remaining HP, and total damage dealt.

### 🧟 Mob Kill Stats Panel
- Sleek top-left HUD panel tracking mob kills for the current world.
- Accurately cropped mob face icons for 36+ hostile and neutral entities.
- Persists across restarts via server-side saved data.

### ❤️ Crosshair Health Indicator
- Dynamic, elevated 2-line nameplate displayed above the target mob under your crosshair.
- **Genuine Minecraft GUI Hearts** rendered directly from the game's texture atlas (`hud/heart/full`, `half`, `container`) alongside crisp `HP/MaxHP` numbers.
- **Full-Bright Rendering**: stays 100% vibrant and clear even under dense forest canopies, caves, or nighttime darkness.
- Extended-range raycasting with precision targeting.

### 🏆 Leaderboard GUI
- Dedicated screen (`O` by default) styled as a modern dark rounded panel:
  - **PvP Tab**: Kills / Deaths / K-D ratio per player.
  - **Mob Kills Tab**: Players ranked by personal mob-kill count.
- High-definition player face avatars with outer skin layers.
- Interactive pagination controls (60 entries/page), smooth scrolling, and self-row highlighting.

### ⚙️ Configuration (Cloth Config)
All settings are easily configurable through the in-game options menu:
- Kill feed display duration and max visible entries.
- Independent toggles for Kill Feed, Mob Stats Panel, and Health Indicator.
- **Server-side**: optional dimension-restricted kill broadcasts.

### ⌨️ Keybinds
| Key | Action |
|-----|--------|
| `K` | Toggle Kill Feed HUD |
| `L` | Toggle Mob Kill Stats Panel |
| `O` | Open Leaderboard Screen |
| `H` | Toggle Crosshair Health Indicator |

Toggling any HUD element plays an audio cue and displays an action-bar confirmation.

### 🌍 Localization
Fully translated into 13 languages:

English · Русский · Беларуская · Українська · Polski · 简体中文 · 繁體中文（台灣）· Deutsch · Svenska · Nederlands · Español · Português (Brasil) · Français

---

## 📋 Requirements

- **Minecraft**: 26.1 (26.1.2)
- **Java**: 25+
- **NeoForge**: 26.1.2.95+ or **Fabric**: Fabric API + Fabric Loader

---

## 🚀 Installation

1. Download the latest `.jar` from [Releases](https://github.com/eliasnvx/Krylix/releases) or [Modrinth](https://modrinth.com/mod/krylix).
2. Install **NeoForge** or **Fabric** for Minecraft 26.1.
3. Place the downloaded file into your `.minecraft/mods/` directory.
4. Launch the game!

---

## 🎮 Commands

| Command | Side | Permission | Description |
|---------|------|------------|-------------|
| `/krylix toggle` | Server | `op` | Enable/disable the kill feed server-wide |
| `/krylix status` | Server | `op` | Show server-side kill feed status |
| `/krylix hud toggle` | Client | — | Toggle the kill feed HUD (same as `K`) |
| `/krylix hud clear` | Client | — | Clear active kill feed entries |
| `/krylix hud count` | Client | — | Show active notification count |
| `/krylix leaderboard testfill [count]` | Client | — | Fill leaderboard with test player entries |
| `/krylix leaderboard clear` | Client | — | Clear test leaderboard data |

---

## 🏗️ Development & Building

```bash
# Clone the repository
git clone https://github.com/eliasnvx/Krylix.git
cd Krylix

# Build all modules (NeoForge and Fabric)
./gradlew build

# Run NeoForge client
./gradlew :neoforge:runClient

# Run Fabric client
./gradlew :fabric:runClient
```

### Multi-Loader Architecture

```
Krylix/
├── buildSrc/                 # Gradle plugins & version constants
├── common/                   # Shared Java 25 logic, models, network packets, translations
├── neoforge/                 # NeoForge 26.1 implementation (EventBus, RenderPipelines, ClientPayLoads)
└── fabric/                   # Fabric 26.1 implementation (Mixins, ClientPlayNetworking)
```

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Credits

- **Lead Developer**: [eliasnvx](https://github.com/eliasnvx)
- **Built with**: [Cloth Config](https://github.com/shedaniel/cloth-config)

<div align="center">

**⭐ Star this repository if you love clean PvP HUDs!**

</div>
