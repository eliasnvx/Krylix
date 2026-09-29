package com.eliasnvx.krylix.server;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Map;

/** One player's all-time numbers in this world. */
public class PlayerStat {
    public static final Codec<PlayerStat> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("lastName", "").forGetter(s -> s.lastName != null ? s.lastName : ""),
            Codec.INT.optionalFieldOf("kills", 0).forGetter(s -> s.kills),
            Codec.INT.optionalFieldOf("deaths", 0).forGetter(s -> s.deaths),
            Codec.INT.optionalFieldOf("mobKills", 0).forGetter(s -> s.mobKills),
            // Added in 1.4: per entity type ("minecraft:zombie" -> 12). 1.3 worlds load it empty.
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("mobKillsByType", Map.of()).forGetter(s -> s.mobKillsByType)
        ).apply(instance, PlayerStat::new)
    );

    public String lastName;
    public int kills;
    public int deaths;
    /** Total hostile mobs killed; equals the sum of {@link #mobKillsByType} for data recorded since 1.4. */
    public int mobKills;
    public final Map<String, Integer> mobKillsByType;

    public PlayerStat(String lastName, int kills, int deaths, int mobKills, Map<String, Integer> mobKillsByType) {
        this.lastName = lastName;
        this.kills = kills;
        this.deaths = deaths;
        this.mobKills = mobKills;
        this.mobKillsByType = new HashMap<>(mobKillsByType);
    }

    public PlayerStat(String lastName) {
        this(lastName, 0, 0, 0, Map.of());
    }
}
