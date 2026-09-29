# Changelog

All notable changes to Krylix are documented here.

## [1.3.1]

A bugfix release for Minecraft 26.1.2 (Fabric and NeoForge).

### Fixed
- **`/krylix toggle` and `/krylix status` now require operator permission.** Before, any player could switch the kill feed off for the whole server.
- **Health plates only replace the nametag of the entity you aim at.** Every other visible nametag (players, named mobs) used to be redrawn with the target's hearts.
- **Health plates no longer reveal hidden players:** no plate for invisible entities, spectators, or players whose team hides its nametags — the same rules as vanilla nametags.
- **Health plates reach 48 blocks in every direction.** The search box collapsed for some view directions, so the plate only worked at melee range there. The target is also found once per tick instead of once per entity every frame.
- **The death recap card no longer covers "You Died!"**, the death message and the score: it sits under the Respawn / Title Screen buttons and shrinks on short windows.
- **Fabric: "Damage Dealt" in the death recap was always 0.** Damage players deal is now tracked on Fabric too; on both loaders old fight records are dropped after 20 seconds instead of piling up.
- **Mob killers showed 20 health in the kill feed** whatever they had left.
- **Zombie Villagers** had a grey placeholder instead of their face; mobs with custom names now pick the longest matching mob name (Cave Spider over Spider, Wither Skeleton over Wither).
- **The config screen is reachable in game** — Mods → Krylix → Config on NeoForge, through Mod Menu on Fabric. It was never registered.
- Missing translations: the death recap title and damage line and the hearts style in 11 languages, the notification count in English and Russian.
- A 26.1 build no longer tries to load on Minecraft 26.2+ (where it would crash); on NeoForge, Cloth Config is now a declared dependency, so a missing Cloth Config gives a clear message instead of a crash.

### Changed
- New mod icon.

---

## [1.3.0]

### Added
- **Minecraft 26.1 & Java 25 Support**:
  - Full support for Minecraft 26.1.2 on both **NeoForge** (26.1.2.95+) and **Fabric** (Fabric API 0.155.2+).
  - Migrated codebase to Java 25.
- **Native GUI Heart Sprites for Mob Health Indicator**:
  - Switched in-world health indicators to native Minecraft GUI heart atlas sprites (`hud/heart/full`, `half`, `container`) combined with crisp numeric HP text (`10/10`).
  - Enforced `LightCoordsUtil.FULL_BRIGHT` for health nameplates, ensuring 100% full bright visibility in caves, dark forests, and at night.
  - Raised nameplate anchor cleanly (+0.35 blocks) above mob models to prevent overlapping horns and heads.
- **Expanded & Pixel-Perfect Mob Head Textures**:
  - Corrected UV coordinates and texture resolutions across 36+ mobs (Zoglin, Hoglin, Skeleton, Creeper, Spider, Enderman, Warden, Breeze, Bogged, Ghast, Witch, Iron Golem, etc.).
  - Migrated HUD entity texture paths to Minecraft's native `textures/entity/` namespace.
- **High-Definition Player Face Avatars**:
  - Integrated native `PlayerFaceExtractor` across the Leaderboard, Kill Feed HUD, and Death Recap screens with support for 3D outer skin layers (hats, hair, accessories).

### Fixed
- Fixed dark/unreadable mob health bars when standing in entity or terrain shadows.
- Fixed distorted/squashed mob head icons in the top-left Mob Stats HUD.
- Fixed scrambled player face avatars in the Leaderboard and Death Recap screens.
- Fixed Hoglin and Zoglin head icons displaying ear slices instead of the face.

---

## [1.1.0]

### Added
- **Player leaderboard GUI** (default key `O`) — a dark rounded-panel screen, not a chat dump. Two tabs: PvP (kills/deaths/K-D) and Mob Kills (players ranked by personal mob-kill count). Scrollable, with self-row highlight, avatars, empty states, and page-switch controls (60 entries/page) that appear once you scroll to the bottom of a page.
- **Mob kill stats HUD panel** (top-left, default key `L`) — tracks hostile-mob kills for the current world, persists across restarts.
- **Health indicator** (default key `H`) — compact HP readout above whatever entity is under your crosshair, using an extended-range raytrace (works past normal interaction distance).
- **Full configuration screen** (Esc → Mods → Krylix → Config).
- **Server-side setting**: restrict kill feed broadcasts to players in the same dimension (default: off).
- **Persistent toggle state** for kill feed, mob stats panel, and health indicator.
- **Toggle feedback**: a click sound and an action-bar ON/OFF confirmation whenever a HUD element is toggled via keybind.
- **Default keybinds**: `K` (kill feed), `L` (mob stats), `O` (leaderboard), `H` (health indicator).
- **Heart icon** in the kill feed replacing the old plain "NNhp" text.
- **Test command**: `/krylix leaderboard testfill [count]`.
- **Localization**: full translations into 13 languages.

---

## [1.0.0]

Initial release: kill feed HUD with player/mob heads, weapon icons, kill streak announcements, and basic project scaffolding.
