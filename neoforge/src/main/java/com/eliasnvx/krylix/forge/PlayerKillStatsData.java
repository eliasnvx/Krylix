package com.eliasnvx.krylix.forge;

import com.eliasnvx.krylix.Krylix;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;

public class PlayerKillStatsData extends SavedData {
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
        DataFixTypes.LEVEL
    );

    public static PlayerKillStatsData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public void recordKill(String killerUuid, String killerName) {
        PlayerStat stat = stats.computeIfAbsent(killerUuid, k -> new PlayerStat());
        stat.kills++;
        stat.lastName = killerName;
        setDirty();
    }

    public void recordDeath(String victimUuid, String victimName) {
        PlayerStat stat = stats.computeIfAbsent(victimUuid, k -> new PlayerStat());
        stat.deaths++;
        stat.lastName = victimName;
        setDirty();
    }

    public void recordMobKill(String killerUuid, String killerName) {
        PlayerStat stat = stats.computeIfAbsent(killerUuid, k -> new PlayerStat());
        stat.mobKills++;
        stat.lastName = killerName;
        setDirty();
    }
}
