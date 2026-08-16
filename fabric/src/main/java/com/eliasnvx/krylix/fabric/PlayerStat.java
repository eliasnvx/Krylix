package com.eliasnvx.krylix.fabric;

public class PlayerStat {
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
}
