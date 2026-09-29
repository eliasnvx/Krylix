<div align="center">

![Krylix](https://raw.githubusercontent.com/eliasnvx/Krylix/26.1-dev/docs/images/banner.png)

[![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2-62B47A?style=for-the-badge&logo=minecraft&logoColor=white)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.19+-DBD0B4?style=for-the-badge)](https://fabricmc.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-26.1.2.95+-E68A00?style=for-the-badge)](https://neoforged.net/)
[![Java](https://img.shields.io/badge/Java-25-B07219?style=for-the-badge)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-D6303C?style=for-the-badge)](https://github.com/eliasnvx/Krylix/blob/26.1-dev/LICENSE)
[![Version](https://img.shields.io/badge/Version-1.3.1-FFC440?style=for-the-badge)](https://github.com/eliasnvx/Krylix/blob/26.1-dev/CHANGELOG.md)

[![Modrinth](https://img.shields.io/badge/Modrinth-Download-1BD96A?style=for-the-badge&logo=modrinth&logoColor=white)](https://modrinth.com/mod/krylix)
[![CurseForge](https://img.shields.io/badge/CurseForge-Download-F16436?style=for-the-badge&logo=curseforge&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/krylix)

**A kill feed, health plates over whatever you aim at, per-world mob stats<br>and an all-time PvP leaderboard — for Fabric and NeoForge.**

[Kill Feed](#kill-feed) • [Health Plates](#health-plates) • [Mob Stats](#mob-stats) • [Leaderboard](#leaderboard) • [Death Recap](#death-recap) • [Config & Keys](#config--keys) • [Languages](#languages) • [Servers](#servers--addons) • [Installation](#installation)

</div>

---

## About

**Krylix** brings the combat information you are used to from shooters into Minecraft, drawn in the game's own style: who killed whom and with what, how much health your target has left, how many of each mob you have taken down in this world, and who is on top of the server — all time.

Everything is one key away and stays out of the way when you don't need it.

**Languages:** English, Русский, Беларуская, Українська, Polski, Deutsch, Nederlands, Svenska, Español, Português (Brasil), Français, 简体中文, 繁體中文

![On your HUD](https://raw.githubusercontent.com/eliasnvx/Krylix/26.1-dev/docs/images/hud.png)

---

![Kill Feed](https://raw.githubusercontent.com/eliasnvx/Krylix/26.1-dev/docs/images/header_killfeed.png)

## Kill Feed

Every player death on the server appears in the top-right corner for a few seconds:

- **Killer and victim heads** — real player skins with the hat layer, or the mob's own face for 36 vanilla mobs
- **The weapon** that did it and **the killer's health** at that moment
- **Badges** — a critical hit, a mace smash, and a long shot (30+ blocks with a bow or trident) with the distance
- **Falls, fire, drowning and other environmental deaths** and self-kills get a clean one-sided row
- Rows fade out after 15 seconds; up to 5 at once (both configurable)
- Servers can limit the feed to players **in the same dimension**

---

![Health Plates](https://raw.githubusercontent.com/eliasnvx/Krylix/26.1-dev/docs/images/header_health.png)

## Health Plates

![Health plates](https://raw.githubusercontent.com/eliasnvx/Krylix/26.1-dev/docs/images/health.png)

Aim at any living entity — up to 48 blocks away — and a plate appears over its head: its name, the game's own **heart sprites** and the exact **HP / max HP**. Plates are drawn full-bright, so they stay readable in caves, under trees and at night. They follow the game's rules: no plate for invisible players or when a team hides its nametags, so nothing leaks in PvP. Press **H** to hide them.

---

![Mob Stats](https://raw.githubusercontent.com/eliasnvx/Krylix/26.1-dev/docs/images/header_mobstats.png)

## Mob Stats

A compact panel in the top-left corner counts the hostile mobs you have killed **in this world**, most-killed first, each with its face. The numbers are kept by the server, survive restarts and are sent to you when you join. Press **L** to hide it.

---

![Leaderboard](https://raw.githubusercontent.com/eliasnvx/Krylix/26.1-dev/docs/images/header_leaderboard.png)

## Leaderboard

![Leaderboard](https://raw.githubusercontent.com/eliasnvx/Krylix/26.1-dev/docs/images/leaderboard.png)

Press **O** for the leaderboard — a proper screen, not a wall of chat:

- **PvP** — kills, deaths and K/D for every player the server has seen
- **Mob Kills** — players ranked by how many mobs they have killed
- Player faces, your own row highlighted, smooth scrolling, 60 players per page with page controls at the bottom
- All-time per world, stored on the server

---

![Death Recap](https://raw.githubusercontent.com/eliasnvx/Krylix/26.1-dev/docs/images/header_recap.png)

## Death Recap

![Death recap](https://raw.githubusercontent.com/eliasnvx/Krylix/26.1-dev/docs/images/recap.png)

When you die, a card under the Respawn button shows **who killed you**, their face, how much **health they had left**, the **damage you dealt them** in the fight, and the same crit / smash / long-shot badges as the feed.

---

![Config & Keys](https://raw.githubusercontent.com/eliasnvx/Krylix/26.1-dev/docs/images/header_config.png)

## Config & Keys

| Key | Action |
|---|---|
| **K** | Show / hide the kill feed |
| **L** | Show / hide the mob stats panel |
| **O** | Open the leaderboard |
| **H** | Show / hide health plates |

All keys can be rebound in *Options → Controls*. Settings are in game — *Mods → Krylix → Config* (on Fabric through [Mod Menu](https://modrinth.com/mod/modmenu)) — and in `config/krylix.json5`:

| Setting | Default | What it does |
|---|---|---|
| `displaySeconds` | `15` | how long a kill feed row stays on screen |
| `maxEntries` | `5` | kill feed rows at once |
| `killFeedEnabled` / `mobStatsEnabled` / `healthIndicatorEnabled` | `true` | the same switches as K / L / H |
| `mobStatsMaxEntries` | `8` | rows in the mob stats panel |
| `restrictBroadcastToSameDimension` | `false` | server: send kills only to players in the same dimension |

**Commands**

| Command | Where | What it does |
|---|---|---|
| `/krylix toggle` | server, op | turn the kill feed on or off for everyone |
| `/krylix status` | server, op | show whether the feed is on and how many rows are active |
| `/krylix hud toggle \| clear \| count` | client | the same as **K**; clear your feed; count its rows. On Fabric the root is `/krylixclient` |

---

![Languages](https://raw.githubusercontent.com/eliasnvx/Krylix/26.1-dev/docs/images/header_languages.png)

## Languages

Krylix is fully translated into **13 languages**: English, Russian, Belarusian, Ukrainian, Polish, German, Dutch, Swedish, Spanish, Brazilian Portuguese, French, Simplified Chinese and Traditional Chinese (Taiwan). Switch the game language and every screen, key name and message follows.

---

![Servers & Addons](https://raw.githubusercontent.com/eliasnvx/Krylix/26.1-dev/docs/images/header_server.png)

## Servers & Addons

- Install Krylix **on the server and on every client**. The server decides what counts as a kill, keeps the statistics and sends the feed, the stats and the recap; health plates are drawn by the client alone.
- Statistics are saved inside the world folder, so they move with the world.
- An **addon API** — custom mob and weapon icons, kill events, extra leaderboard columns — is planned for the Minecraft 26.3 release.

---

![Installation](https://raw.githubusercontent.com/eliasnvx/Krylix/26.1-dev/docs/images/header_install.png)

## Installation

| | Fabric | NeoForge |
|---|---|---|
| Minecraft | 26.1.2 | 26.1.2 |
| Loader | [Fabric Loader](https://fabricmc.net/) 0.19+ and [Fabric API](https://modrinth.com/mod/fabric-api) | [NeoForge](https://neoforged.net/) 26.1.2.95+ |
| [Cloth Config](https://modrinth.com/mod/cloth-config) | required | required |
| [Mod Menu](https://modrinth.com/mod/modmenu) | optional (Config button) | — |

1. Install the loader, Fabric API (on Fabric) and Cloth Config for Minecraft 26.1.2.
2. Download Krylix from [Modrinth](https://modrinth.com/mod/krylix), [CurseForge](https://www.curseforge.com/minecraft/mc-mods/krylix) or the [releases](https://github.com/eliasnvx/Krylix/releases).
3. Drop the `.jar` into `mods` — on the client **and** the server.

---

## Development

```bash
./gradlew build                                        # Fabric and NeoForge jars
./gradlew :fabric:runClient                            # dev client (Fabric)
./gradlew :neoforge:runClient                          # dev client (NeoForge)
KRYLIX_DOCS_SHOTS=1 ./gradlew :fabric:runClientGameTest # README screenshots, taken in a real game
python3 tools/docs/make_readme_images.py               # banner, headers and galleries in docs/images
```

Multi-loader: `common` / `fabric` / `neoforge`, Java 25, Mojang names. Changes: [`CHANGELOG.md`](https://github.com/eliasnvx/Krylix/blob/26.1-dev/CHANGELOG.md).

The pictures on this page are real in-game screenshots taken by a client GameTest (`fabric/src/gametest`); the banner, headers and frames are drawn by `tools/docs/make_readme_images.py`.

---

## Credits & License

- **Author:** [eliasnvx](https://github.com/eliasnvx)
- **Built with:** [Cloth Config](https://github.com/shedaniel/cloth-config)

[MIT](https://github.com/eliasnvx/Krylix/blob/26.1-dev/LICENSE) © eliasnvx

<div align="center">

**Made for everyone who wants to know who got the last hit**

</div>
