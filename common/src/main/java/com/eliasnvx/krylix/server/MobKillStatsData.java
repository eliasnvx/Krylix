package com.eliasnvx.krylix.server;

import com.eliasnvx.krylix.Krylix;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;

/**
 * The 1.3 world-wide mob kill pool. Only read, once, by {@link PlayerKillStatsData} to move a singleplayer world's
 * counts to its player; new kills are counted per player.
 */
public class MobKillStatsData extends SavedData {
    public final Map<String, Integer> kills = new HashMap<>();

    public static final Codec<MobKillStatsData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("kills", Map.of()).forGetter(d -> d.kills)
        ).apply(instance, map -> {
            MobKillStatsData data = new MobKillStatsData();
            data.kills.putAll(map);
            return data;
        })
    );

    public static final SavedDataType<MobKillStatsData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(Krylix.MOD_ID, "mob_stats"),
        MobKillStatsData::new,
        CODEC,
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );
}
