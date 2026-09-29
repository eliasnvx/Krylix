package com.eliasnvx.krylix.fabric.gametest;

import com.eliasnvx.krylix.api.KrylixApi;
import com.eliasnvx.krylix.api.KrylixClientApi;
import com.eliasnvx.krylix.api.client.HealthProvider;
import com.eliasnvx.krylix.api.client.MobHead;
import com.eliasnvx.krylix.api.event.StatRecordedEvent;
import com.eliasnvx.krylix.api.stats.PlayerStats;
import com.eliasnvx.krylix.api.stats.StatType;
import com.eliasnvx.krylix.client.KillFeedHud;
import com.eliasnvx.krylix.client.KrylixClientApiImpl;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/**
 * The addon API end to end in a real game: {@link TestKrylixAddon} is found through its entrypoint, its listeners
 * see real kills, its cancel is honoured, and its heads and health provider are used.
 */
public final class AddonApiClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        check(TestKrylixAddon.initialized && TestKrylixAddon.clientInitialized, "test addon was not initialized");
        check("1.0.0".equals(KrylixApi.get().apiVersion()), "api version " + KrylixApi.get().apiVersion());

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("gamerule spawn_mobs false");
            UUID me = context.computeOnClient(mc -> mc.player.getUUID());

            // A mob kill by the player: credit + kill events, a MOB_KILL stat, saved statistics
            world.getServer().runCommand("summon minecraft:zombie ~ ~ ~3 {NoAI:1b,Tags:[\"api\"]}");
            world.getServer().runCommand("damage @e[tag=api,limit=1] 1000 minecraft:player_attack by @p");
            context.waitTicks(5);
            check(TestKrylixAddon.credits.get() >= 1, "no KillCreditEvent");
            check(TestKrylixAddon.kills.contains("minecraft:zombie"), "no KillEvent for the zombie: " + TestKrylixAddon.kills);
            StatRecordedEvent stat = TestKrylixAddon.stats.getLast();
            check(stat.type() == StatType.MOB_KILL && stat.total() == 1 && me.equals(stat.player())
                && Identifier.withDefaultNamespace("zombie").equals(stat.entityType()), "unexpected stat " + stat);
            int mobKills = world.getServer().computeOnServer(server ->
                KrylixApi.get().stats(server).get(me).map(PlayerStats::mobKills).orElse(-1));
            check(mobKills == 1, "stats view says " + mobKills + " mob kills");

            // A cancelled KillEvent: the kill is not counted
            world.getServer().runCommand("summon minecraft:zombie ~ ~ ~3 {NoAI:1b,Tags:[\"api\",\"" + TestKrylixAddon.IGNORED_TAG + "\"]}");
            world.getServer().runCommand("damage @e[tag=api,limit=1] 1000 minecraft:player_attack by @p");
            context.waitTicks(5);
            int afterIgnored = world.getServer().computeOnServer(server ->
                KrylixApi.get().stats(server).get(me).map(PlayerStats::mobKills).orElse(-1));
            check(afterIgnored == 1, "a cancelled kill was counted: " + afterIgnored);

            // Health provider, asked by the health plate
            world.getServer().runCommand("summon minecraft:husk ~ ~ ~3 {NoAI:1b,Tags:[\"probe\"],CustomName:\"" + TestKrylixAddon.HEALTH_PROBE + "\"}");
            world.getServer().runCommand("summon minecraft:husk ~2 ~ ~3 {NoAI:1b,Tags:[\"probe\"]}");
            context.waitTicks(5);
            int husks = context.computeOnClient(mc -> {
                int seen = 0;
                for (var entity : mc.level.entitiesForRendering()) {
                    if (entity instanceof LivingEntity living && net.minecraft.world.entity.EntityType.getKey(entity.getType()).getPath().equals("husk")) {
                        HealthProvider.Health health = KrylixClientApiImpl.healthOf(living);
                        boolean probe = living.hasCustomName();
                        check(probe ? health.current() == 7 && health.max() == 9 : health.max() == living.getMaxHealth(),
                            (probe ? "probe" : "plain") + " husk got " + health);
                        seen++;
                    }
                }
                return seen;
            });
            check(husks == 2, "the client saw " + husks + " husks");
            world.getServer().runCommand("kill @e[tag=probe]");

            // Heads: shipped JSON, code-registered, and the resource pack winning over code
            context.runOnClient(mc -> {
                KrylixClientApi api = KrylixClientApi.get();
                check(api.mobHead(Identifier.withDefaultNamespace("sulfur_cube")).isPresent(), "no sulfur cube head");
                check(api.mobHead(Identifier.withDefaultNamespace("giant")).isPresent(), "no code-registered giant head");
                MobHead zombie = api.mobHead(Identifier.withDefaultNamespace("zombie")).orElseThrow();
                check(!zombie.layers().getFirst().texture().equals(TestKrylixAddon.OVERRIDDEN_ZOMBIE_TEXTURE),
                    "a code head won over the resource pack one");
            });

            // A player death reaches the client as a feed row, through FeedEntryEvent
            context.runOnClient(mc -> KillFeedHud.clear());
            world.getServer().runCommand("kill @p");
            context.waitForScreen(DeathScreen.class);
            context.waitTicks(5);
            check(!TestKrylixAddon.feedRows.isEmpty(), "no FeedEntryEvent");
            check(TestKrylixAddon.feedRows.getLast().victim().isPlayer(), "feed row victim is not the player");
            check(context.computeOnClient(mc -> KillFeedHud.count()) == 1, "feed row missing from the HUD");
            check(TestKrylixAddon.stats.stream().anyMatch(s -> s.type() == StatType.DEATH && me.equals(s.player())), "no DEATH stat");
            context.runOnClient(mc -> {
                mc.player.respawn();
                mc.gui.setScreen(null);
            });
            context.waitTicks(10);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
