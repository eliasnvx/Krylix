package com.eliasnvx.krylix.forge;

public class PlayerStat {
    public int kills = 0;
    public int deaths = 0;
    public int mobKills = 0;
    public String lastName = "";

    public PlayerStat() {}

    public PlayerStat(int kills, int deaths, int mobKills, String lastName) {
        this.kills = kills;
        this.deaths = deaths;
        this.mobKills = mobKills;
        this.lastName = lastName;
    }
}
