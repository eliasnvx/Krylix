# Changelog

All notable changes to Krylix are documented here.

## [1.1.0]

### Added
- **Player leaderboard GUI** (default key `O`) — a dark rounded-panel screen, not a chat dump. Two tabs: PvP (kills/deaths/K-D) and Mob Kills (players ranked by personal mob-kill count). Scrollable, with self-row highlight, avatars, empty states, and page-switch controls (60 entries/page) that appear once you scroll to the bottom of a page.
- **Mob kill stats HUD panel** (top-left, default key `L`) — tracks hostile-mob kills for the current world, persists across restarts.
- **Health indicator** (default key `H`) — compact HP readout above whatever entity is under your crosshair, using an extended-range raytrace (works past normal interaction distance). 4 configurable bar styles: Blocks, ASCII, Dots, Number Only.
- **Full configuration screen** (Esc → Mods → Krylix → Config) — previously registered but never wired into Forge's mod list; now actually reachable in-game. Covers kill feed timing/limits, panel toggles, health bar style, and the server-side broadcast restriction.
- **Server-side setting**: restrict kill feed broadcasts to players in the same dimension (default: off).
- **Persistent toggle state** for kill feed, mob stats panel, and health indicator — no longer reset on restart.
- **Toggle feedback**: a click sound and an action-bar ON/OFF confirmation whenever a HUD element is toggled via keybind.
- **Default keybinds**: `K` (kill feed), `L` (mob stats), `O` (leaderboard), `H` (health indicator) — bound out of the box, no setup required.
- **Heart icon** in the kill feed replacing the old plain "NNhp" text.
- **Test command**: `/krylix leaderboard testfill [count]` (1–500, client-only, visual) to preview scrolling/pagination without needing real players.
- **Localization**: full translations into 13 languages — English, Russian, Belarusian, Ukrainian, Polish, Simplified Chinese, Traditional Chinese (Taiwan), German, Swedish, Dutch, Spanish, Brazilian Portuguese, French.
- **5 additional verified mob face textures**: Enderman (composited from the base + glowing-eyes overlay texture, since the base alone is nearly featureless black), Ravager, Warden, Wither, Zombie Villager — bringing verified mob coverage to 27. Guardian, Shulker, and Phantom were evaluated the same way and excluded again — their front-face crop doesn't read as a recognizable face.

### Fixed
- Iron Golem and other unmapped mobs showing a random player skin instead of a neutral fallback.
- Spider/cave spider showing the wrong body part instead of the face (wrong UV assumption).
- Duplicate kill feed rendering from two independently-registered render hooks.
- Kill distance calculation missing a square root.
- Weapon detection matching on localized display name instead of registry id (broke on non-English clients).
- Self-kill / environmental death entries showing a confusing blank killer avatar.
- Rank numbers overlapping avatars in the leaderboard for 3-digit ranks.
- Next-page arrow rendering off-center (Minecraft's `<`/`>` glyphs aren't pixel-mirror-symmetric) — replaced with a hand-drawn, geometrically mirrored pixel arrow.
- Flickering/highlighted text when the health indicator's own name line collided with vanilla's built-in nametag for custom-named entities under the crosshair.
- Leaderboard tab labels, column headers, and footer text were hardcoded in English and ignored the selected language.
- "Mob Kills" leaderboard tab originally listed mob *types* instead of *players* ranked by mob-kill count.
- `/krylix toggle`, `/krylix status`, `/krylix hud ...`, and `/krylix leaderboard ...` command feedback was hardcoded in English; now localized.

### Removed
- The kill streak system (Double Kill → Godlike chat announcements and escalating sounds) — added noise without enough value, removed entirely along with its config option and internal tracking.
- The `/testkill` command tree (`sword`/`bow`/`axe`/`headshot`/`multi`) — synthetic test-data scaffolding with no end-user purpose.

### Changed
- Renamed all packages from the `com.example.modid` template to `com.eliasnvx.krylix`.
- Removed the unused Fabric module entirely — Forge only going forward.
- Skin textures are now cached instead of re-queried every frame.
- Kill feed avatars shrunk and given rounded corners.
- Mod description and metadata broadened to reflect the full feature set (previously described only the kill feed).

## [1.0.0]

Initial release: kill feed HUD with player/mob heads, weapon icons, kill streak announcements, and basic Forge+Kotlin project scaffolding.
