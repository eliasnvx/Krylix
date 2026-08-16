package com.eliasnvx.krylix.forge;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public class PlayerKillStatsData extends SavedData {
    public final Map<String, PlayerStat> stats = new HashMap<>();
    private static final String ID = "krylix_player_stats";

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

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag statsTag = new CompoundTag();
        for (Map.Entry<String, PlayerStat> entry : stats.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putInt("kills", entry.getValue().kills);
            entryTag.putInt("deaths", entry.getValue().deaths);
            entryTag.putInt("mobKills", entry.getValue().mobKills);
            entryTag.putString("name", entry.getValue().lastName);
            statsTag.put(entry.getKey(), entryTag);
        }
        tag.put("stats", statsTag);
        return tag;
    }

    private static PlayerKillStatsData load(CompoundTag tag, HolderLookup.Provider registries) {
        PlayerKillStatsData data = new PlayerKillStatsData();
        CompoundTag statsTag = tag.getCompound("stats");
        for (String uuid : statsTag.getAllKeys()) {
            CompoundTag entryTag = statsTag.getCompound(uuid);
            data.stats.put(uuid, new PlayerStat(
                    entryTag.getInt("kills"),
                    entryTag.getInt("deaths"),
                    entryTag.getInt("mobKills"),
                    entryTag.getString("name")
            ));
        }
        return data;
    }

    public static PlayerKillStatsData get(MinecraftServer server) {
        SavedData.Factory<PlayerKillStatsData> factory = new SavedData.Factory<>(PlayerKillStatsData::new, PlayerKillStatsData::load, null);
        return server.overworld().getDataStorage().computeIfAbsent(factory, ID);
    }
}
