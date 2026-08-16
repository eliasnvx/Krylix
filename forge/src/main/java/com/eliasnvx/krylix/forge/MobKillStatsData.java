package com.eliasnvx.krylix.forge;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public class MobKillStatsData extends SavedData {
    public final Map<String, Integer> kills = new HashMap<>();
    private static final String ID = "krylix_mob_stats";

    public Map<String, Integer> increment(String entityId) {
        kills.put(entityId, kills.getOrDefault(entityId, 0) + 1);
        setDirty();
        return kills;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag killsTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : kills.entrySet()) {
            killsTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("kills", killsTag);
        return tag;
    }

    private static MobKillStatsData load(CompoundTag tag, HolderLookup.Provider registries) {
        MobKillStatsData data = new MobKillStatsData();
        CompoundTag killsTag = tag.getCompound("kills");
        for (String key : killsTag.getAllKeys()) {
            data.kills.put(key, killsTag.getInt(key));
        }
        return data;
    }

    public static MobKillStatsData get(MinecraftServer server) {
        SavedData.Factory<MobKillStatsData> factory = new SavedData.Factory<>(MobKillStatsData::new, MobKillStatsData::load, null);
        return server.overworld().getDataStorage().computeIfAbsent(factory, ID);
    }
}
