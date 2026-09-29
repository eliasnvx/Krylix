package com.eliasnvx.krylix.server;

import com.eliasnvx.krylix.addon.KrylixApiImpl;
import com.eliasnvx.krylix.api.event.KillEvent;
import com.eliasnvx.krylix.api.event.StatRecordedEvent;
import com.eliasnvx.krylix.api.stats.StatType;
import com.eliasnvx.krylix.config.KrylixConfig;
import com.eliasnvx.krylix.model.KillFlags;
import com.eliasnvx.krylix.network.KrylixNetwork;
import com.eliasnvx.krylix.network.KrylixPayloads.DeathRecapPayload;
import com.eliasnvx.krylix.network.KrylixPayloads.KillFeedPayload;
import com.eliasnvx.krylix.network.KrylixPayloads.LeaderboardPayload;
import com.eliasnvx.krylix.network.KrylixPayloads.LeaderboardRow;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side Krylix: the loaders forward their events here. Runs on the server thread only.
 */
public final class KrylixServer {
    private static final long LEADERBOARD_REQUEST_COOLDOWN_MS = 1_000;

    private static final CombatLog COMBAT = new CombatLog();
    private static final Map<UUID, Long> lastLeaderboardRequest = new HashMap<>();
    /** Switched by /krylix toggle; lasts until the server stops. */
    private static boolean feedEnabled = true;

    private KrylixServer() {
    }

    public static boolean isFeedEnabled() {
        return feedEnabled;
    }

    public static void setFeedEnabled(boolean enabled) {
        feedEnabled = enabled;
    }

    /** After a living entity took damage (after armor and shields). */
    public static void onDamage(LivingEntity target, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player attacker && amount > 0) {
            COMBAT.record(attacker.getUUID(), target.getUUID(), amount, System.currentTimeMillis());
        }
    }

    /** After a living entity died. */
    public static void onDeath(LivingEntity victim, DamageSource source) {
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        MinecraftServer server = level.getServer();
        KillAnalysis kill = KillAnalysis.of(victim, source);
        boolean playerDied = victim instanceof ServerPlayer;
        boolean mobKill = !playerDied && victim.getType().getCategory() == MobCategory.MONSTER && kill.killer() instanceof ServerPlayer;
        if (!playerDied && !mobKill) {
            return;
        }

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
            sendDeathRecap(player, source, kill);
            if (recordStats) {
                int deaths = stats.recordDeath(player.getUUID(), player.getName().getString());
                statRecorded(player, StatType.DEATH, null, deaths);
                if (kill.killer() instanceof Player killer) {
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

    private static void statRecorded(Player player, StatType type, @Nullable Identifier entityType, int total) {
        if (KrylixApiImpl.EVENTS.hasListeners(StatRecordedEvent.class)) {
            KrylixApiImpl.EVENTS.post(new StatRecordedEvent(player.getUUID(), player.getName().getString(), type, entityType, total));
        }
    }

    public static void onJoin(ServerPlayer player) {
        PlayerKillStatsData stats = PlayerKillStatsData.get(player.level().getServer());
        KrylixNetwork.toPlayer(player, new MobKillsPayload(true, stats.mobKillsOf(player.getUUID())));
    }

    public static void onLeave(ServerPlayer player) {
        COMBAT.forget(player.getUUID());
        lastLeaderboardRequest.remove(player.getUUID());
    }

    public static void onLeaderboardRequest(ServerPlayer player) {
        long now = System.currentTimeMillis();
        Long last = lastLeaderboardRequest.get(player.getUUID());
        if (last != null && now - last < LEADERBOARD_REQUEST_COOLDOWN_MS) {
            return;
        }
        lastLeaderboardRequest.put(player.getUUID(), now);

        PlayerKillStatsData stats = PlayerKillStatsData.get(player.level().getServer());
        List<LeaderboardRow> rows = new ArrayList<>(stats.stats.size());
        for (Map.Entry<String, PlayerStat> entry : stats.stats.entrySet()) {
            UUID uuid;
            try {
                uuid = UUID.fromString(entry.getKey());
            } catch (IllegalArgumentException e) {
                continue;
            }
            PlayerStat s = entry.getValue();
            rows.add(new LeaderboardRow(uuid, s.lastName, s.kills, s.deaths, s.mobKills));
        }
        if (rows.size() > KrylixPayloads.MAX_LEADERBOARD_ROWS) {
            // Keep the players who matter on either tab: top by kills, then fill with top by mob kills
            rows.sort(Comparator.comparingInt(LeaderboardRow::kills).reversed());
            List<LeaderboardRow> kept = new ArrayList<>(rows.subList(0, KrylixPayloads.MAX_LEADERBOARD_ROWS / 2));
            rows.sort(Comparator.comparingInt(LeaderboardRow::mobKills).reversed());
            for (LeaderboardRow row : rows) {
                if (kept.size() >= KrylixPayloads.MAX_LEADERBOARD_ROWS) break;
                if (!kept.contains(row)) kept.add(row);
            }
            rows = kept;
        }
        KrylixNetwork.toPlayer(player, new LeaderboardPayload(rows, stats.stats.size()));
    }

    private static void sendDeathRecap(ServerPlayer victim, DamageSource source, KillAnalysis kill) {
        LivingEntity killer = kill.killer();
        float dealt = killer != null ? COMBAT.dealt(victim.getUUID(), killer.getUUID(), System.currentTimeMillis()) : 0f;
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
