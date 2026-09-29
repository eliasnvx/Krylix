package com.eliasnvx.krylix.fabric.gametest;

import com.eliasnvx.krylix.client.KillFeedHud;
import com.eliasnvx.krylix.client.LeaderboardClient;
import com.eliasnvx.krylix.client.LeaderboardScreen;
import com.eliasnvx.krylix.model.KillEntry;
import com.eliasnvx.krylix.network.KrylixPayloads;
import com.eliasnvx.krylix.network.KrylixPayloads.Combatant;
import com.eliasnvx.krylix.network.KrylixPayloads.LeaderboardRow;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.tutorial.TutorialSteps;

/**
 * Screenshots for the README and the mod pages (docs/images). Skipped unless {@code KRYLIX_DOCS_SHOTS=1}:
 * {@code KRYLIX_DOCS_SHOTS=1 ./gradlew :fabric:runClientGameTest}, then {@code python3 tools/docs/make_readme_images.py}.
 *
 * <p>Everything on the pictures is rendered by the mod itself. Mob kills are real (the mob stats panel and the
 * leaderboard's mob column come from the server); the kill feed rows and the other leaderboard players are fed to
 * the client directly, because a singleplayer world has only one player to fight.
 */
public final class DocsShotsClientTest implements FabricClientGameTest {

    private static final int SETTLE = 20;

    @Override
    public void runTest(ClientGameTestContext context) {
        if (System.getenv("KRYLIX_DOCS_SHOTS") == null) {
            return;
        }
        context.getInput().resizeWindow(1920, 1080);
        context.runOnClient(mc -> {
            mc.options.tutorialStep = TutorialSteps.NONE;
            mc.options.guiScale().set(3);
            mc.options.fov().set(70);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        });

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            cmd(world, "difficulty normal");
            cmd(world, "gamerule spawn_mobs false");
            cmd(world, "gamerule advance_time false");
            cmd(world, "gamerule advance_weather false");
            cmd(world, "time set 5000");
            cmd(world, "weather clear");
            // The held item's name pops up for a few seconds after a change: equip it first
            cmd(world, "item replace entity @p weapon.mainhand with minecraft:netherite_sword");
            meadow(world);
            context.waitTicks(SETTLE * 4);

            // Real kills: the server counts them and syncs the mob stats panel and our leaderboard row
            killMobs(world, "zombie", 14, "");
            killMobs(world, "skeleton", 9, "");
            killMobs(world, "spider", 6, "");
            killMobs(world, "creeper", 5, "");
            killMobs(world, "enderman", 3, "");
            killMobs(world, "witch", 2, "");
            cmd(world, "kill @e[type=minecraft:item]");
            cmd(world, "kill @e[type=minecraft:experience_orb]");

            // The fight: a zombie in reach with a health plate, a skeleton and a creeper behind it
            cmd(world, "summon minecraft:zombie ~0.4 ~ ~2.7 {NoAI:1b,PersistenceRequired:1b,Tags:[\"hero\"],Rotation:[180f,0f],"
                    + "equipment:{head:{id:\"minecraft:iron_helmet\",count:1},mainhand:{id:\"minecraft:iron_shovel\",count:1}}}");
            cmd(world, "damage @e[tag=hero,sort=nearest,limit=1] 7 minecraft:generic");
            cmd(world, "summon minecraft:skeleton ~-3.5 ~ ~9 {NoAI:1b,PersistenceRequired:1b,Rotation:[160f,0f],"
                    + "equipment:{head:{id:\"minecraft:chainmail_helmet\",count:1},mainhand:{id:\"minecraft:bow\",count:1}}}");
            cmd(world, "summon minecraft:creeper ~4 ~ ~11 {NoAI:1b,PersistenceRequired:1b,Rotation:[200f,0f]}");
            face(context, world, "@e[tag=hero,sort=nearest,limit=1]");
            feed(context);
            shot(context, world, "docs_hero");

            // Health plate close-up on a vindicator
            gone(world, "@e[tag=hero]");
            context.runOnClient(mc -> KillFeedHud.clear()); // the plate is big up close
            cmd(world, "summon minecraft:vindicator ~-0.3 ~ ~2.4 {NoAI:1b,PersistenceRequired:1b,Tags:[\"plate\"],Rotation:[180f,0f],"
                    + "equipment:{mainhand:{id:\"minecraft:iron_axe\",count:1}}}");
            cmd(world, "damage @e[tag=plate,sort=nearest,limit=1] 9 minecraft:generic");
            face(context, world, "@e[tag=plate,sort=nearest,limit=1]");
            shot(context, world, "docs_health");
            gone(world, "@e[tag=plate]");

            // Death recap: we hit a zombie with a sword, it gets the last hit
            cmd(world, "summon minecraft:zombie ~0.6 ~ ~2.2 {NoAI:1b,PersistenceRequired:1b,Tags:[\"killer\"],Rotation:[180f,0f],"
                    + "equipment:{head:{id:\"minecraft:golden_helmet\",count:1},mainhand:{id:\"minecraft:iron_sword\",count:1}}}");
            face(context, world, "@e[tag=killer,sort=nearest,limit=1]");
            // We land a hit first, so the recap has damage dealt to show
            cmd(world, "damage @e[tag=killer,sort=nearest,limit=1] 7 minecraft:player_attack by @p");
            context.waitTicks(SETTLE / 2);
            cmd(world, "damage @p 1000 minecraft:mob_attack by @e[tag=killer,sort=nearest,limit=1]");
            context.waitForScreen(DeathScreen.class);
            context.waitTicks(SETTLE * 4); // let the death poof clear
            context.takeScreenshot("docs_recap");
            context.runOnClient(mc -> {
                mc.player.respawn();
                mc.gui.setScreen(null);
            });
            context.waitTicks(SETTLE);
            gone(world, "@e[tag=killer]");
            context.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));

            // Leaderboard: our real row plus a server full of other players. The screen asks the server for the table
            // when it opens, so the made-up rows go in after that reply has arrived.
            context.setScreen(LeaderboardScreen::new);
            context.waitTicks(SETTLE / 2);
            context.runOnClient(mc -> {
                List<LeaderboardRow> rows = leaderboard(mc.player.getUUID(), mc.player.getName().getString());
                LeaderboardClient.set(rows, rows.size());
            });
            context.getInput().setCursorPos(8, 8); // opening a screen centers the cursor: move it off the rows
            context.waitTicks(SETTLE / 2);
            context.takeScreenshot("docs_leaderboard_pvp");
            context.runOnClient(mc -> ((LeaderboardScreen) mc.gui.screen()).showMobKills());
            context.waitTicks(SETTLE / 2);
            context.takeScreenshot("docs_leaderboard_mobs");
            context.setScreen(() -> null);
        }
    }

    // ------------------------------------------------------------------------------------------------ scene

    private static void cmd(TestSingleplayerContext world, String command) {
        world.getServer().runCommand(command);
    }

    /** Kills {@code count} fresh mobs behind the camera with a player hit, so Krylix records them like any kill. */
    private static void killMobs(TestSingleplayerContext world, String type, int count, String nbt) {
        for (int i = 0; i < count; i++) {
            cmd(world, "summon minecraft:" + type + " ~" + (i % 4) + " ~ ~-" + (24 + i / 4) + " {NoAI:1b,Tags:[\"fodder\"]" + nbt + "}");
        }
        // /damage takes a single target
        cmd(world, "execute as @e[tag=fodder] run damage @s 1000 minecraft:player_attack by @p");
    }

    /** A small meadow in front of the camera (the camera looks south, +z): trees, flowers and grass. */
    private static void meadow(TestSingleplayerContext world) {
        int[][] trees = {{-7, 12}, {6, 15}, {-12, 20}, {11, 9}, {1, 22}, {-3, 17}};
        for (int[] t : trees) {
            cmd(world, "fill ~" + t[0] + " ~ ~" + t[1] + " ~" + t[0] + " ~4 ~" + t[1] + " minecraft:oak_log");
            cmd(world, "fill ~" + (t[0] - 2) + " ~3 ~" + (t[1] - 2) + " ~" + (t[0] + 2) + " ~4 ~" + (t[1] + 2) + " minecraft:oak_leaves[persistent=true] replace air");
            cmd(world, "fill ~" + (t[0] - 1) + " ~5 ~" + (t[1] - 1) + " ~" + (t[0] + 1) + " ~5 ~" + (t[1] + 1) + " minecraft:oak_leaves[persistent=true]");
        }
        String[] flowers = {"minecraft:short_grass", "minecraft:short_grass", "minecraft:poppy", "minecraft:dandelion",
                "minecraft:short_grass", "minecraft:cornflower", "minecraft:oxeye_daisy", "minecraft:short_grass"};
        long seed = 42;
        for (int i = 0; i < 90; i++) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            int x = (int) ((seed >>> 33) % 26) - 13;
            int z = (int) ((seed >>> 13) % 24) + 1;
            if (Math.abs(x) <= 1 && z <= 4) {
                continue; // keep the line of fire clear
            }
            cmd(world, "setblock ~" + x + " ~ ~" + z + " " + flowers[(int) ((seed >>> 50) % flowers.length)] + " keep");
        }
    }

    /** Looks at the target's eyes from the player's eyes (the server's own command anchor is its feet). */
    private static void face(ClientGameTestContext context, TestSingleplayerContext world, String target) {
        cmd(world, "execute as @p at @s anchored eyes run tp @s ~ ~ ~ facing entity " + target + " eyes");
        context.waitTicks(SETTLE / 2);
    }

    /** Removes entities without death poofs or drops. */
    private static void gone(TestSingleplayerContext world, String selector) {
        cmd(world, "tp " + selector + " ~ -200 ~");
    }

    private static void shot(ClientGameTestContext context, TestSingleplayerContext world, String name) {
        context.runOnClient(mc -> {
            mc.gui.toastManager().clear();
            mc.gui.hud.getChat().clearMessages(false);
        });
        context.waitTicks(SETTLE / 2);
        context.takeScreenshot(name);
    }

    // ------------------------------------------------------------------------------------------------ fed data

    private static UUID offline(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    private static Combatant player(String name) {
        return new Combatant(name, offline(name), "minecraft:player");
    }

    /** Four rows of a busy PvP server: a long bow shot, a mob kill, a mace smash and a crit. */
    private static void feed(ClientGameTestContext context) {
        context.runOnClient(mc -> {
            KillFeedHud.clear();
            long now = System.currentTimeMillis();
            Combatant me = new Combatant(mc.player.getName().getString(), mc.player.getUUID(), "minecraft:player");
            KillFeedHud.add(new KillEntry(player("NightOwl"), player("Pixel_Knight"), 12f, "minecraft:bow", 42f, KrylixPayloads.FLAG_LONGSHOT, now));
            KillFeedHud.add(new KillEntry(new Combatant("Skeleton", null, "minecraft:skeleton"), player("Vortex_"), 20f, "minecraft:bow", 17f, 0, now));
            KillFeedHud.add(new KillEntry(player("Kira"), player("NightOwl"), 6f, "minecraft:mace", 2f, KrylixPayloads.FLAG_SMASH, now));
            KillFeedHud.add(new KillEntry(me, player("Vortex_"), 17f, "minecraft:netherite_sword", 3f, KrylixPayloads.FLAG_CRITICAL, now));
        });
    }

    private static List<LeaderboardRow> leaderboard(UUID myId, String me) {
        Object[][] rows = {
                {"Vortex_", 48, 21, 212}, {"NightOwl", 41, 17, 164}, {"Pixel_Knight", 33, 25, 301},
                {"Kira", 29, 9, 97}, {"RedstoneRaven", 22, 30, 140}, {"Mossy", 18, 14, 256},
                {"Blaze_Runner", 15, 19, 88}, {"SnowFox", 11, 7, 73}, {"TNT_Tim", 9, 22, 41},
                {"Lumen", 6, 4, 190}, {"QuietStep", 3, 11, 35},
        };
        List<LeaderboardRow> list = new ArrayList<>();
        for (Object[] r : rows) {
            list.add(new LeaderboardRow(offline((String) r[0]), (String) r[0], (int) r[1], (int) r[2], (int) r[3]));
        }
        // Our own row: the 39 real mob kills from this run, PvP numbers to sit mid-table
        list.add(new LeaderboardRow(myId, me, 37, 12, 39));
        return list;
    }
}
