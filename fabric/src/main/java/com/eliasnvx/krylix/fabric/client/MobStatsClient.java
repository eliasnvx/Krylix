package com.eliasnvx.krylix.fabric.client;

import com.eliasnvx.krylix.Krylix;

import java.util.HashMap;
import java.util.Map;

public class MobStatsClient {
    private static Map<String, Integer> currentStats = new HashMap<>();

    public static void updateStats(Map<String, Integer> stats) {
        currentStats = new HashMap<>(stats);
        Krylix.LOGGER.debug("Client mob stats updated: " + currentStats.size() + " entries");
    }

    public static Map<String, Integer> getStats() {
        return currentStats;
    }
}
