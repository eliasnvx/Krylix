# Krylix - Minecraft Kill Feed Mod

## Project Overview

**Krylix** is a modern kill feed mod for Minecraft inspired by CS:GO and Valorant.

- **Package:** `io.eliasnvx.krylix`
- **Language:** Kotlin 1.9.22+ (NOT Java)
- **Target:** Minecraft 1.21+
- **Platforms:** Fabric & Forge via Architectury
- **Developer:** Russian-speaking (English for public docs)

## Tech Stack

### Core
- **Kotlin** 1.9.22 with coroutines
- **Architectury** for multi-loader support
- **Minecraft** 1.21 (official Mojang mappings)
- **Fabric API** + Fabric Language Kotlin
- **Kotlinx Serialization** for config (NOT gson)

### Build
- Gradle with Kotlin DSL
- Architectury Plugin + Loom
- Multi-module Gradle project

## Project Structure

```
Krylix/
├── common/                           # Platform-independent code
│   └── src/main/kotlin/io/eliasnvx/krylix/
│       ├── Krylix.kt                # Main mod object
│       ├── config/
│       │   └── KrylixConfig.kt      # Config with kotlinx.serialization
│       ├── event/
│       │   └── DeathEventHandler.kt # Death event logic
│       ├── model/
│       │   └── KillEntry.kt         # Data class for kill entries
│       ├── render/
│       │   ├── KillFeedRenderer.kt  # HUD rendering
│       │   └── AnimationHelper.kt   # Animation logic
│       └── network/
│           └── KrylixPackets.kt     # Multiplayer sync
├── fabric/                          # Fabric-specific
│   └── src/main/kotlin/io/eliasnvx/krylix/fabric/
│       └── KrylixFabric.kt          # Fabric entry point
├── forge/                           # Forge-specific
│   └── src/main/kotlin/io/eliasnvx/krylix/forge/
│       └── KrylixForge.kt           # Forge entry point
└── build.gradle.kts                 # Root build file
```

## Critical Rules

### 1. Language Choice
🔥 **ALWAYS use Kotlin** - This is NOT a Java project
- Use data classes for models
- Use `object` for singletons
- Prefer `val` over `var`
- Use extension functions
- Leverage Kotlin idioms (let, apply, run, etc.)

### 2. Architectury Pattern
**Common module:**
- Platform-agnostic code ONLY
- No direct Fabric/Forge API usage
- Define interfaces for platform behavior

**Fabric/Forge modules:**
- Platform-specific implementations ONLY
- Event registration
- Entry points

### 3. Minecraft Rendering (1.21+)
- Use `DrawContext` for HUD rendering
- Store render state in model classes
- Update animations per frame
- Clean up expired entries immediately

### 4. Performance
- Limit max entries (default: 5)
- Remove expired entries to prevent leaks
- Avoid heavy operations in render loop
- Cache frequently used values

## Coding Standards

### Kotlin Style

✅ **DO:**
```kotlin
// Data classes for models
data class KillEntry(
    val killerName: String,
    val victimName: String,
    val weapon: ItemStack,
    val distance: Double? = null
)

// Objects for singletons
object Krylix {
    const val MOD_ID = "krylix"
    fun init() { /* ... */ }
}

// Extension functions
fun Player.addKillToFeed(victim: Player) {
    KillFeedRenderer.addEntry(/*...*/)
}

// Type-safe builders
fun buildKillEntry(builder: KillEntry.Builder.() -> Unit): KillEntry
```

❌ **DON'T:**
```kotlin
// Java-style getters/setters
public class KillEntry {
    private String killerName;
    public String getKillerName() { return killerName; }
}

// Mutable public collections
class Renderer {
    val entries: MutableList<KillEntry> = mutableListOf()
}

// Platform code in common module
// common/src/.../Krylix.kt
import net.fabricmc.api.ModInitializer // ❌
```

### Naming Conventions
- **Classes:** PascalCase (`KillFeedRenderer`)
- **Functions:** camelCase (`updateAnimation`)
- **Constants:** UPPER_SNAKE_CASE (`MAX_ENTRIES`)
- **Packages:** lowercase (`io.eliasnvx.krylix.render`)
- **Files:** Match class name (`KillEntry.kt`)

### File Organization
- One primary class per file
- Group by feature, not type
- Keep platform code in platform modules
- Use internal visibility where appropriate

## Configuration

### KrylixConfig.kt
```kotlin
@Serializable
data class KrylixConfig(
    val maxEntries: Int = 5,
    val fadeTime: Int = 5,
    val showDistance: Boolean = true,
    val position: Position = Position.TOP_RIGHT
) {
    companion object {
        private val json = Json { 
            prettyPrint = true
            ignoreUnknownKeys = true
        }
        
        fun load(): KrylixConfig {
            val file = File("config/krylix.json")
            return if (file.exists()) {
                json.decodeFromString(file.readText())
            } else {
                KrylixConfig().also { save(it) }
            }
        }
    }
}
```

## Build Configuration

### build.gradle.kts (root)
```kotlin
plugins {
    kotlin("jvm") version "1.9.22"
    id("architectury-plugin") version "3.4-SNAPSHOT"
    id("dev.architectury.loom") version "1.5-SNAPSHOT" apply false
    id("kotlinx-serialization") version "1.9.22"
}

architectury {
    minecraft = "1.21"
}

allprojects {
    group = "io.eliasnvx"
    version = "1.0.0"
}
```

### Essential Dependencies
- `minecraft` 1.21
- `fabric-language-kotlin` >= 1.10.0
- `kotlinx-serialization-json`
- Official Mojang mappings (NOT yarn/MCP)

## Development Commands

```bash
# Run Fabric in dev
./gradlew :fabric:runClient

# Run Forge in dev
./gradlew :forge:runClient

# Build all jars
./gradlew build

# Clean build
./gradlew clean build

# Generate IDE sources
./gradlew genSources
```

## Testing Checklist

- [ ] Kill detection in singleplayer
- [ ] Kill detection in multiplayer
- [ ] Config save/load
- [ ] Animations smooth
- [ ] Void deaths (no attacker)
- [ ] Self-kills (fall damage)
- [ ] Projectile kills
- [ ] Distance calculation
- [ ] Memory cleanup (no leaks)

## Modrinth Publishing

### Required Files
- `icon.png` (512x512, transparent)
- Screenshots/GIFs
- Detailed description
- Changelog

### Metadata
- **Name:** Krylix
- **Slug:** krylix
- **Categories:** Utility, HUD
- **Side:** Client-only
- **Versions:** 1.21+
- **License:** MIT

### Description Template
```markdown
# Krylix

**Next-generation kill feed for Minecraft**

Brings professional FPS-style kill notifications to Minecraft PvP.

## Features
- 🎯 Real-time kill notifications
- 🎨 Smooth fade animations
- 🔧 Fully customizable
- 📊 Distance tracking
- 💀 Headshot detection
- 🎮 Lightweight & optimized

## Installation
1. Install Fabric or Forge
2. Download Krylix
3. Place in mods folder
4. Launch!
```

## Communication Style

- Use **Russian** naturally for private notes/comments
- Use **English** for public docs, commits, READMEs
- Be direct and specific when asking questions
- Technical terms can be in English (e.g., "render loop")

## Git Workflow

### .gitignore
```
build/
.gradle/
run/
*.iml
.idea/
.vscode/
*.log
CLAUDE.local.md
.windsurf/
```

### Commit Messages (English)
- `feat: add kill feed rendering`
- `fix: headshot detection not working`
- `refactor: improve animation system`
- `docs: update README`
- `perf: optimize render loop`

## Resource Files

### fabric.mod.json
```json
{
  "schemaVersion": 1,
  "id": "krylix",
  "version": "1.0.0",
  "name": "Krylix",
  "description": "Next-gen kill feed for Minecraft",
  "authors": ["eliasnvx"],
  "contact": {
    "homepage": "https://modrinth.com/mod/krylix",
    "sources": "https://github.com/eliasnvx/Krylix"
  },
  "license": "MIT",
  "icon": "assets/krylix/icon.png",
  "environment": "client",
  "entrypoints": {
    "client": [
      {
        "adapter": "kotlin",
        "value": "io.eliasnvx.krylix.fabric.KrylixFabric"
      }
    ]
  },
  "depends": {
    "fabricmc": ">=0.15.0",
    "fabric-language-kotlin": ">=1.10.0",
    "minecraft": ">=1.21"
  }
}
```

## Edge Cases to Handle

### Death Detection
- **Void deaths:** No attacker entity
- **Environmental:** Lava, fire, drowning
- **Self-damage:** Fall damage, magic
- **Projectiles:** Arrow shooter tracking
- **Explosions:** TNT, creepers

### Multiplayer
- Sync kills to all clients
- Handle lag/packet loss
- Respect server settings
- Don't spam network

### Rendering
- Handle window resize
- Different aspect ratios
- Chat overlay conflicts
- Other HUD mods compatibility

## Performance Considerations

1. **Memory Management**
   - Remove expired entries immediately
   - Don't store full Player objects
   - Cache frequently accessed data

2. **Render Optimization**
   - Batch draw calls where possible
   - Use matrix stack efficiently
   - Avoid string formatting in hot path

3. **Event Handling**
   - Debounce rapid events
   - Async processing where possible
   - Cancel old animations when new ones start

## Known Issues to Avoid

❌ **Platform mixing:** Don't put Fabric code in common
❌ **Java patterns:** Use Kotlin idioms
❌ **Null safety:** Leverage Kotlin's type system
❌ **Mutable state:** Prefer immutability
❌ **String concatenation:** Use string templates

## When You Need Help

1. **Kotlin questions:** Check kotlinlang.org
2. **Minecraft APIs:** Reference Fabric/Forge docs
3. **Architectury:** Check architectury.dev
4. **Mod examples:** Look for Kotlin Fabric mods

## Final Reminders

🔥 **This is a Kotlin project** - Use Kotlin idioms
🔥 **Test both loaders** - Fabric AND Forge
🔥 **Performance matters** - Renders every frame
🔥 **Clean common code** - No platform APIs
🔥 **Comments in Russian OK** - For private code

---

**Цель:** Сделать Krylix быстрым, стильным и надежным kill feed модом, который работает как часы на любом сервере.