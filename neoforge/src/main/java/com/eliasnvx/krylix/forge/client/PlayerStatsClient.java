package com.eliasnvx.krylix.forge.client;

import com.eliasnvx.krylix.forge.network.NetworkPackets.PlayerStatEntry;

import java.util.ArrayList;
import java.util.List;

public class PlayerStatsClient {
    private static List<PlayerStatEntry> entries = new ArrayList<>();

    public static void updateStats(List<PlayerStatEntry> newEntries) {
        entries = newEntries;
    }

    public static List<PlayerStatEntry> sortedByKills() {
        List<PlayerStatEntry> sorted = new ArrayList<>(entries);
        sorted.sort((a, b) -> Integer.compare(b.kills(), a.kills()));
        return sorted;
    }

    public static List<PlayerStatEntry> sortedByMobKills() {
        List<PlayerStatEntry> sorted = new ArrayList<>(entries);
        sorted.sort((a, b) -> Integer.compare(b.mobKills(), a.mobKills()));
        return sorted;
    }
}
