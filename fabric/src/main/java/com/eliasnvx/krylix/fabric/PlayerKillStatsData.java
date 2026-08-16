package com.eliasnvx.krylix.fabric;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public class PlayerKillStatsData extends SavedData {
    public final Map<String, PlayerStat> stats = new HashMap<>();

    public static final SavedData.Factory<PlayerKillStatsData> FACTORY = new SavedData.Factory<>(
        PlayerKillStatsData::new,
        PlayerKillStatsData::load,
        null
    );

    public static PlayerKillStatsData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "krylix_player_stats");
    }

    public static PlayerKillStatsData load(CompoundTag tag, HolderLookup.Provider provider) {
        PlayerKillStatsData data = new PlayerKillStatsData();
        CompoundTag statsTag = tag.getCompound("Stats");
        for (String key : statsTag.getAllKeys()) {
            CompoundTag pTag = statsTag.getCompound(key);
            String name = pTag.getString("Name");
            int kills = pTag.getInt("Kills");
            int deaths = pTag.getInt("Deaths");
            int mobKills = pTag.getInt("MobKills");
            data.stats.put(key, new PlayerStat(name, kills, deaths, mobKills));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag statsTag = new CompoundTag();
        for (Map.Entry<String, PlayerStat> entry : stats.entrySet()) {
            CompoundTag pTag = new CompoundTag();
            pTag.putString("Name", entry.getValue().lastName);
            pTag.putInt("Kills", entry.getValue().kills);
            pTag.putInt("Deaths", entry.getValue().deaths);
            pTag.putInt("MobKills", entry.getValue().mobKills);
            statsTag.put(entry.getKey(), pTag);
        }
        tag.put("Stats", statsTag);
        return tag;
    }

    public void recordKill(String uuid, String name) {
        PlayerStat stat = stats.computeIfAbsent(uuid, k -> new PlayerStat(name, 0, 0, 0));
        stat.lastName = name;
        stat.kills++;
        setDirty();
    }

    public void recordDeath(String uuid, String name) {
        PlayerStat stat = stats.computeIfAbsent(uuid, k -> new PlayerStat(name, 0, 0, 0));
        stat.lastName = name;
        stat.deaths++;
        setDirty();
    }

    public void recordMobKill(String uuid, String name) {
        PlayerStat stat = stats.computeIfAbsent(uuid, k -> new PlayerStat(name, 0, 0, 0));
        stat.lastName = name;
        stat.mobKills++;
        setDirty();
    }
}
