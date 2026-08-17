package com.eliasnvx.krylix.fabric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class PlayerStat {
    public static final Codec<PlayerStat> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("lastName", "").forGetter(s -> s.lastName != null ? s.lastName : ""),
            Codec.INT.optionalFieldOf("kills", 0).forGetter(s -> s.kills),
            Codec.INT.optionalFieldOf("deaths", 0).forGetter(s -> s.deaths),
            Codec.INT.optionalFieldOf("mobKills", 0).forGetter(s -> s.mobKills)
        ).apply(instance, PlayerStat::new)
    );

    public String lastName;
    public int kills;
    public int deaths;
    public int mobKills;

    public PlayerStat(String lastName, int kills, int deaths, int mobKills) {
        this.lastName = lastName;
        this.kills = kills;
        this.deaths = deaths;
        this.mobKills = mobKills;
    }

    public PlayerStat() {
        this("", 0, 0, 0);
    }
}
