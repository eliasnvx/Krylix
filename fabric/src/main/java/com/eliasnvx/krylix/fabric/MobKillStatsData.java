package com.eliasnvx.krylix.fabric;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public class MobKillStatsData extends SavedData {
    public final Map<String, Integer> kills = new HashMap<>();

    public static final SavedData.Factory<MobKillStatsData> FACTORY = new SavedData.Factory<>(
        MobKillStatsData::new,
        MobKillStatsData::load,
        null
    );

    public static MobKillStatsData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "krylix_mob_stats");
    }

    public static MobKillStatsData load(CompoundTag tag, HolderLookup.Provider provider) {
        MobKillStatsData data = new MobKillStatsData();
        CompoundTag killsTag = tag.getCompound("Kills");
        for (String key : killsTag.getAllKeys()) {
            data.kills.put(key, killsTag.getInt(key));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag killsTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : kills.entrySet()) {
            killsTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("Kills", killsTag);
        return tag;
    }

    public Map<String, Integer> increment(String entityId) {
        kills.put(entityId, kills.getOrDefault(entityId, 0) + 1);
        setDirty();
        return kills;
    }
}
