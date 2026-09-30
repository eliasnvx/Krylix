package com.eliasnvx.krylix.server;

import com.eliasnvx.krylix.Krylix;
import com.eliasnvx.krylix.addon.KrylixApiImpl;
import com.eliasnvx.krylix.platform.KrylixPlatform;
import com.eliasnvx.krylix.api.event.KillEvent;
import com.eliasnvx.krylix.api.event.StatRecordedEvent;
import com.eliasnvx.krylix.api.stats.StatType;
import com.eliasnvx.krylix.config.KrylixConfig;
import com.eliasnvx.krylix.model.KillFlags;
import com.eliasnvx.krylix.network.KrylixNetwork;
import com.eliasnvx.krylix.network.KrylixPayloads.DeathRecapPayload;
import com.eliasnvx.krylix.network.KrylixPayloads.KillFeedPayload;
import com.eliasnvx.krylix.network.KrylixPayloads.LeaderboardPayload;
import com.eliasnvx.krylix.network.KrylixPayloads.MobKillsPayload;
import com.eliasnvx.krylix.network.KrylixPayloads;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side Krylix: the loaders forward their events here. Runs on the server thread only.
 */
public final class KrylixServer {
    private static final int LEADERBOARD_REQUEST_COOLDOWN_TICKS = 20;
    /** A changed table is rebuilt at most this often; everyone asking in between gets the same payload. */
    private static final int LEADERBOARD_REBUILD_TICKS = 40;

    private static final CombatLog COMBAT = new CombatLog();
    private static final Map<UUID, Integer> lastLeaderboardRequest = new HashMap<>();
    private static @Nullable LeaderboardPayload leaderboard;
    private static long leaderboardVersion = -1;
    private static int leaderboardBuiltAt;
    /** Switched by /krylix toggle; lasts until the server stops. Read by the API from any thread. */
    private static volatile boolean feedEnabled = true;

    private KrylixServer() {
    }

    public static boolean isFeedEnabled() {
        return feedEnabled;
    }

    public static void setFeedEnabled(boolean enabled) {
        feedEnabled = enabled;
    }

    /** The server stopped: nothing may carry over to the next world or server in this JVM (singleplayer). */
    public static void onServerStopped() {
        COMBAT.clear();
        lastLeaderboardRequest.clear();
        leaderboard = null;
        leaderboardVersion = -1;
        feedEnabled = true;
    }

    /**
     * A living entity lost health: the amount after armor, enchantments and absorption, as recorded in its combat
     * tracker. Never throws: Krylix must not break vanilla damage handling.
     */
    public static void onDamage(LivingEntity target, DamageSource source, float amount) {
        try {
            if (amount > 0 && source.getEntity() instanceof Player attacker && target.level() instanceof ServerLevel level) {
                COMBAT.record(attacker.getUUID(), target.getUUID(), amount, level.getServer().getTickCount());
            }
        } catch (RuntimeException e) {
            Krylix.LOGGER.error("Krylix failed to record damage to {}", target, e);
        }
    }

    /** After a living entity died. Never throws: Krylix must not break vanilla death handling. */
    public static void onDeath(LivingEntity victim, DamageSource source) {
        try {
            handleDeath(victim, source);
        } catch (RuntimeException e) {
            Krylix.LOGGER.error("Krylix failed to handle the death of {}", victim, e);
        }
    }

    private static void handleDeath(LivingEntity victim, DamageSource source) {
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        // Only two kinds of death matter; skip the analysis (and addon events) for the cows and the fish
        boolean playerDied = victim instanceof ServerPlayer player && isRealPlayer(player);
        if (!playerDied && victim.getType().getCategory() != MobCategory.MONSTER) {
            return;
        }
        KillAnalysis kill = KillAnalysis.of(victim, source);
        if (!playerDied && !(kill.killer() instanceof ServerPlayer killer && isRealPlayer(killer))) {
            return;
        }
        MinecraftServer server = level.getServer();

        boolean broadcast = true;
        boolean recordStats = true;
        if (KrylixApiImpl.EVENTS.hasListeners(KillEvent.class)) {
            KillEvent event = KrylixApiImpl.EVENTS.post(new KillEvent(victim, kill.killer(), source,
                Identifier.parse(kill.weapon()), kill.distance(), KillFlags.fromBits(kill.flags())));
            if (event.isCancelled()) {
                return;
            }
            broadcast = event.broadcast();
            recordStats = event.recordStats();
            kill = new KillAnalysis(kill.killer(), victim, event.weapon().toString(), kill.distance(), KillFlags.toBits(event.flags()));
        }

        PlayerKillStatsData stats = PlayerKillStatsData.get(server);
        if (victim instanceof ServerPlayer player) {
            sendDeathRecap(player, source, kill, server.getTickCount());
            if (recordStats) {
                int deaths = stats.recordDeath(player.getUUID(), player.getName().getString());
                statRecorded(player, StatType.DEATH, null, deaths);
                if (kill.killer() instanceof ServerPlayer killer && isRealPlayer(killer)) {
                    int kills = stats.recordKill(killer.getUUID(), killer.getName().getString());
                    statRecorded(killer, StatType.KILL, null, kills);
                }
            }
            if (broadcast) {
                broadcastFeed(level, kill);
            }
        } else if (recordStats && kill.killer() instanceof ServerPlayer killer) {
            Identifier type = EntityType.getKey(victim.getType());
            int count = stats.recordMobKill(killer.getUUID(), killer.getName().getString(), type.toString());
            KrylixNetwork.toPlayer(killer, new MobKillsPayload(false, Map.of(type.toString(), count)));
            statRecorded(killer, StatType.MOB_KILL, type, count);
        }
    }

    /** Machines that act as players (deployers, mob grinders) don't get statistics. */
    private static boolean isRealPlayer(ServerPlayer player) {
        return !KrylixPlatform.get().isFakePlayer(player);
    }

    private static void statRecorded(Player player, StatType type, @Nullable Identifier entityType, int total) {
        if (KrylixApiImpl.EVENTS.hasListeners(StatRecordedEvent.class)) {
            KrylixApiImpl.EVENTS.post(new StatRecordedEvent(player.getUUID(), player.getName().getString(), type, entityType, total));
        }
    }

    public static void onJoin(ServerPlayer player) {
        if (!isRealPlayer(player)) {
            return;
        }
        PlayerKillStatsData stats = PlayerKillStatsData.get(player.level().getServer());
        stats.updateName(player.getUUID(), player.getName().getString()); // renamed since their last kill
        KrylixNetwork.toPlayer(player, new MobKillsPayload(true, stats.mobKillsOf(player.getUUID())));
    }

    public static void onLeave(ServerPlayer player) {
        COMBAT.forget(player.getUUID());
        lastLeaderboardRequest.remove(player.getUUID());
    }

    public static void onLeaderboardRequest(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        int tick = server.getTickCount();
        Integer last = lastLeaderboardRequest.get(player.getUUID());
        if (last != null && tick - last >= 0 && tick - last < LEADERBOARD_REQUEST_COOLDOWN_TICKS) {
            return;
        }
        lastLeaderboardRequest.put(player.getUUID(), tick);
        KrylixNetwork.toPlayer(player, leaderboard(server, tick));
    }

    /** The table as a payload, rebuilt only when the statistics changed (and not more often than every 2 s). */
    private static LeaderboardPayload leaderboard(MinecraftServer server, int tick) {
        PlayerKillStatsData stats = PlayerKillStatsData.get(server);
        LeaderboardPayload cached = leaderboard;
        boolean stale = stats.version() != leaderboardVersion
            && (tick - leaderboardBuiltAt >= LEADERBOARD_REBUILD_TICKS || tick < leaderboardBuiltAt);
        if (cached != null && !stale) {
            return cached;
        }
        leaderboard = new LeaderboardPayload(stats.leaderboardRows(KrylixPayloads.MAX_LEADERBOARD_ROWS), stats.stats.size());
        leaderboardVersion = stats.version();
        leaderboardBuiltAt = tick;
        return leaderboard;
    }

    private static void sendDeathRecap(ServerPlayer victim, DamageSource source, KillAnalysis kill, int tick) {
        LivingEntity killer = kill.killer();
        float dealt = killer != null ? COMBAT.dealt(victim.getUUID(), killer.getUUID(), tick) : 0f;
        KrylixNetwork.toPlayer(victim, new DeathRecapPayload(
            kill.killerCombatant(),
            source.getLocalizedDeathMessage(victim).getString(),
            killer != null ? killer.getHealth() : 0f,
            killer != null ? killer.getMaxHealth() : 0f,
            kill.weapon(),
            kill.distance(),
            dealt,
            kill.flags()
        ));
    }

    private static void broadcastFeed(ServerLevel level, KillAnalysis kill) {
        if (!feedEnabled) {
            return;
        }
        KillFeedPayload payload = new KillFeedPayload(
            kill.killerCombatant(),
            kill.victimCombatant(),
            kill.killer() != null ? kill.killer().getHealth() : 0f,
            kill.weapon(),
            kill.distance(),
            kill.flags()
        );
        if (KrylixConfig.get().restrictBroadcastToSameDimension) {
            KrylixNetwork.toDimension(level, payload);
        } else {
            KrylixNetwork.toAll(level.getServer(), payload);
        }
    }
}
