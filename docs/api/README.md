# Krylix API: addon guide (API 1.0.0)

Krylix's addon API lets other mods change who gets a kill and what the feed shows, keep their own statistics, and
add mob faces and health numbers for their entities. Mob faces need no code at all: a JSON file in a resource pack is
enough.

- [Setup](#setup)
- [Entry point](#entry-point)
- [Mob faces without code](#mob-faces-without-code)
- [Events](#events)
- [Kill credit](#kill-credit)
- [Kills: hide, don't count, restyle](#kills-hide-dont-count-restyle)
- [Statistics](#statistics)
- [Feed rows on the client](#feed-rows-on-the-client)
- [Mob faces from code](#mob-faces-from-code)
- [Health providers](#health-providers)
- [Rules](#rules)

A working addon for both loaders is in [`example-addon`](../../example-addon): friendly fire, a practice mode,
kill milestones, absorption hearts on the health plate and a quieter feed.

## Setup

Compile against the API jar only. Everything outside `com.eliasnvx.krylix.api` is internal and can change in any
release. At runtime you need the full mod; the API classes are inside it, so players install nothing extra.

```groovy
repositories {
    maven { url "https://api.modrinth.com/maven" } // the full mod, for dev runs
    // + the Maven repository the API is published to (see the mod page)
}

dependencies {
    compileOnly "com.eliasnvx:krylix-api:1.0.0+26.3"

    // Fabric module: the mod at runtime
    runtimeOnly "maven.modrinth:krylix:1.4.0+26.3-fabric"
    // NeoForge module
    runtimeOnly "maven.modrinth:krylix:1.4.0+26.3-neoforge"
}
```

Most addons work without Krylix too, so declare it as optional: the entrypoint simply never runs without it.

- Fabric `fabric.mod.json`: `"suggests": { "krylix": ">=1.4.0" }` (or `depends`)
- NeoForge `neoforge.mods.toml`: `[[dependencies.<your_mod>]]`, `modId = "krylix"`, `type = "optional"`, `ordering = "AFTER"`

Check the API version at runtime with `KrylixApi#apiVersion()`. Minor versions only add things.

## Entry point

```java
@RegisterKrylixAddon                           // NeoForge: found by annotation scan
public final class MyKrylixAddon implements KrylixAddon {
    @Override public void onInitialize(KrylixApi api) { /* event listeners */ }
    @Override public void onInitializeClient(KrylixClientApi api) { /* mob faces, health providers, client events */ }
}
```

On Fabric, add the entrypoint to `fabric.mod.json`: `"entrypoints": { "krylix": ["com.example.MyKrylixAddon"] }`.
A multi-loader mod does both on the same class.

`onInitialize` runs on both sides once Krylix is set up; `onInitializeClient` runs after it, on the physical client
only. Register mob faces and health providers inside `onInitializeClient`: the registries close afterwards and a late
registration throws. An addon that throws is logged and skipped; the others still load.

## Mob faces without code

Krylix draws a face for each mob in the kill feed, the mob panel and the death recap. Faces are JSON files in resource
packs, one per entity type, at `assets/<namespace>/krylix/heads/<path>.json` for entity `<namespace>:<path>`:

```json
{
  "layers": [
    { "texture": "mymod:textures/entity/goblin.png", "u": 8, "v": 8, "width": 8, "height": 8,
      "texture_width": 64, "texture_height": 64 }
  ]
}
```

- `u`, `v`: the top-left pixel of the face in the texture; `width`/`height`: its size in pixels (default 8).
  `texture_width`/`texture_height`: the texture's size (default 64). The region is scaled into a square.
- Layers are drawn in order: put glowing eyes or an overlay in a second layer (Krylix's enderman does).
- Ship the file in your mod's resources, or in any resource pack: modpacks can add faces for mods that have none.
- Krylix's own faces for vanilla mobs live in `assets/minecraft/krylix/heads/`, so a resource pack can restyle them.

Without a face, Krylix draws a plain grey tile.

## Events

`api.events()` is one loader-independent bus for both sides:

```java
api.events().addListener(KillEvent.class, event -> { ... });
api.events().addListener(KillEvent.class, EventPriority.HIGH, event -> { ... });
```

- Listeners run on the thread that posts the event: server events on the server thread, client events on the client
  thread.
- `EventPriority.HIGHEST` runs first. Once a `CancellableEvent` is cancelled, lower priorities are not called.
- Events are matched by exact class. A listener that throws is logged and skipped.

| Event | Side | Cancel | When |
|---|---|---|---|
| `KillCreditEvent` | server | – | a living entity died; decide who gets the kill |
| `KillEvent` | server | yes | a player died, or a player killed a hostile mob; before the feed and the statistics |
| `StatRecordedEvent` | server | – | a player's kill, death or mob-kill count went up (after saving) |
| `FeedEntryEvent` | client | yes | a feed row arrived and is about to be shown |

## Kill credit

`KillCreditEvent` comes with Krylix's answer already filled in: the attacker; else whoever knocked the victim off a
ledge, into lava or the void (vanilla's kill credit); with a tamed pet's (or an evoker's vex's) kill credited to its
owner. Change it for your own mechanics:

```java
// A turret block's arrow: credit the player who placed the turret
api.events().addListener(KillCreditEvent.class, event -> {
    if (event.source().getDirectEntity() instanceof MyTurretArrow arrow && arrow.placer() != null) {
        event.setKiller(arrow.placer());
    }
});
```

`setKiller(null)` makes it a death without a killer: the feed shows the victim alone and no kill is counted (the
example addon does this for teammates). When the killer is not the entity that dealt the blow, the feed shows the damage
cause (a feather for a fall, a lava bucket for lava) instead of a weapon.

## Kills: hide, don't count, restyle

`KillEvent` covers the deaths Krylix acts on: a player died (feed row, the victim's death recap, kill and death
counts), or a player killed a hostile mob (the killer's mob statistics).

```java
api.events().addListener(KillEvent.class, event -> {
    if (inMinigame(event.victim())) {
        event.setBroadcast(false);   // no feed row
        event.setRecordStats(false); // no kill, no death counted
    }
    if (event.source().is(MyDamageTypes.LASER)) {
        event.setWeapon(Identifier.fromNamespaceAndPath("mymod", "laser_gun")); // the item shown in the feed
        event.flags().add(KillFlag.LONGSHOT);                                    // the badge
    }
});
```

`cancel()` makes Krylix ignore the death completely, death recap included.

## Statistics

```java
api.events().addListener(StatRecordedEvent.class, event -> {
    if (event.type() == StatType.KILL && event.total() % 100 == 0) {
        rewards.give(event.player(), "100_kills");
    }
});
```

Read the saved numbers with `api.stats(server)` on the server thread: `get(uuid)` for one player, `all()` for
everyone. `PlayerStats` is a snapshot: kills, deaths, mob kills in total and by entity type, and `killDeathRatio()`.

## Feed rows on the client

```java
api.events().addListener(FeedEntryEvent.class, event -> {
    if (event.victim().name().startsWith("[Dummy]")) {
        event.cancel(); // not shown
    }
});
```

The event carries what the server sent: both participants (name, UUID for players, entity type), the weapon item,
the distance and the flags.

## Mob faces from code

For faces that can't be a static JSON file:

```java
api.registerMobHead(Identifier.fromNamespaceAndPath("mymod", "goblin"),
    MobHead.of(Identifier.fromNamespaceAndPath("mymod", "textures/entity/goblin.png"), 8, 8, 8, 8, 64, 64));
```

A face from a resource pack always wins over one registered in code, so players can restyle every face.
`api.mobHead(entityType)` returns the face Krylix will draw.

## Health providers

The health plate over the targeted entity reads `LivingEntity#getHealth()` and `getMaxHealth()`. If your entities
keep their health elsewhere, register a provider:

```java
api.healthProviders().register(Identifier.fromNamespaceAndPath("mymod", "boss_parts"), entity ->
    entity instanceof MyBossPart part ? new HealthProvider.Health(part.boss().health(), part.boss().maxHealth()) : null);
```

Providers are asked in registration order and the first non-null answer wins; vanilla health is the fallback. They run
every frame the plate is shown, so keep them cheap. Health is in half-hearts, like vanilla.

## Rules

- Compile only against `com.eliasnvx.krylix.api`. `api.internal` and everything outside the API package are not for
  addons.
- Register during your addon's init methods; the registries close afterwards.
- Don't call `KrylixApi.get()` from static initializers: it throws before Krylix has initialized. Use the instance
  passed to your addon.
- Server events and `stats(server)`: server thread only. Client API and client events: client thread only.
- The API follows SemVer (`apiVersion()`), separately from the mod's version.
