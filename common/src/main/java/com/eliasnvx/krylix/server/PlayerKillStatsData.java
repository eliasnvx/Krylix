package com.eliasnvx.krylix.server;

import com.eliasnvx.krylix.Krylix;
import com.eliasnvx.krylix.api.stats.KrylixStats;
import com.eliasnvx.krylix.api.stats.PlayerStats;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * All-time PvP and mob-kill numbers per player, saved with the world (overworld data folder, so worlds upgraded
 * from 26.1 keep their file where it was).
 */
public class PlayerKillStatsData extends SavedData {
    /** Keyed by UUID string: the format 1.3 wrote, kept so old worlds load unchanged. */
    public final Map<String, PlayerStat> stats = new HashMap<>();

    public static final Codec<PlayerKillStatsData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.unboundedMap(Codec.STRING, PlayerStat.CODEC).optionalFieldOf("stats", Map.of()).forGetter(d -> d.stats)
        ).apply(instance, map -> {
            PlayerKillStatsData data = new PlayerKillStatsData();
            data.stats.putAll(map);
            return data;
        })
    );

    public static final SavedDataType<PlayerKillStatsData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(Krylix.MOD_ID, "player_stats"),
        PlayerKillStatsData::new,
        CODEC,
        // Arbitrary mod data: the command-storage fixer leaves it alone (LEVEL would run level.dat fixers over it)
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    public static PlayerKillStatsData get(MinecraftServer server) {
        PlayerKillStatsData data = server.overworld().getDataStorage().computeIfAbsent(TYPE);
        data.migrateLegacyMobStats(server);
        return data;
    }

    private boolean legacyChecked;

    /**
     * 1.3 counted mob kills in one pool for the whole world. When that pool exists and exactly one player has
     * stats (singleplayer), it is theirs: move it into their per-type counts. On a multiplayer world the pool can't
     * be split fairly, so it is left untouched.
     */
    private void migrateLegacyMobStats(MinecraftServer server) {
        if (legacyChecked) {
            return;
        }
        legacyChecked = true;
        MobKillStatsData legacy = server.overworld().getDataStorage().get(MobKillStatsData.TYPE);
        if (legacy == null || legacy.kills.isEmpty() || stats.size() != 1) {
            return;
        }
        PlayerStat only = stats.values().iterator().next();
        if (!only.mobKillsByType.isEmpty()) {
            return;
        }
        only.mobKillsByType.putAll(legacy.kills);
        legacy.kills.clear();
        legacy.setDirty();
        setDirty();
        Krylix.LOGGER.info("Moved 1.3 world mob kill counts to player {}", only.lastName);
    }

    private PlayerStat stat(UUID uuid, String name) {
        PlayerStat stat = stats.computeIfAbsent(uuid.toString(), k -> new PlayerStat(name));
        stat.lastName = name;
        return stat;
    }

    /** Returns the player's new kill total. */
    public int recordKill(UUID uuid, String name) {
        int kills = ++stat(uuid, name).kills;
        setDirty();
        return kills;
    }

    /** Returns the player's new death total. */
    public int recordDeath(UUID uuid, String name) {
        int deaths = ++stat(uuid, name).deaths;
        setDirty();
        return deaths;
    }

    /** Returns the player's new total for this entity type. */
    public int recordMobKill(UUID uuid, String name, String entityType) {
        PlayerStat stat = stat(uuid, name);
        stat.mobKills++;
        int count = stat.mobKillsByType.merge(entityType, 1, Integer::sum);
        setDirty();
        return count;
    }

    /** Read-only snapshots for the addon API. */
    public KrylixStats view() {
        return new KrylixStats() {
            @Override
            public Optional<PlayerStats> get(UUID player) {
                PlayerStat stat = stats.get(player.toString());
                return stat != null ? Optional.of(snapshot(player, stat)) : Optional.empty();
            }

            @Override
            public List<PlayerStats> all() {
                List<PlayerStats> all = new ArrayList<>(stats.size());
                for (Map.Entry<String, PlayerStat> entry : stats.entrySet()) {
                    try {
                        all.add(snapshot(UUID.fromString(entry.getKey()), entry.getValue()));
                    } catch (IllegalArgumentException ignored) {
                        // not a UUID: skip, like the leaderboard does
                    }
                }
                return all;
            }
        };
    }

    private static PlayerStats snapshot(UUID uuid, PlayerStat stat) {
        Map<Identifier, Integer> byType = new HashMap<>();
        stat.mobKillsByType.forEach((type, count) -> {
            Identifier id = Identifier.tryParse(type);
            if (id != null) {
                byType.put(id, count);
            }
        });
        return new PlayerStats(uuid, stat.lastName, stat.kills, stat.deaths, stat.mobKills, byType);
    }

    public Map<String, Integer> mobKillsOf(UUID uuid) {
        PlayerStat stat = stats.get(uuid.toString());
        return stat != null ? stat.mobKillsByType : Map.of();
    }
}
