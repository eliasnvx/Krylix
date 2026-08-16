package com.eliasnvx.krylix.fabric.client;

import com.eliasnvx.krylix.fabric.network.FabricNetworkPackets.PlayerStatEntry;

import java.util.ArrayList;
import java.util.List;

public class PlayerStatsClient {
    private static List<PlayerStatEntry> entries = new ArrayList<>();

    public static void updateStats(List<PlayerStatEntry> newEntries) {
        entries = newEntries;
    }

    public static List<PlayerStatEntry> getStats() {
        return entries;
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
