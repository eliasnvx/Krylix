package com.eliasnvx.krylix.client;

import com.eliasnvx.krylix.network.KrylixPayloads.MobKillsPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** This player's own mob kills by entity type, as the server sent them. Client thread only. */
public final class MobStatsClient {
    private static final Map<String, Integer> counts = new HashMap<>();
    /** Most-killed first; rebuilt when counts change, not every frame. */
    private static List<Map.Entry<String, Integer>> sorted = List.of();

    private MobStatsClient() {
    }

    public static void apply(MobKillsPayload payload) {
        if (payload.replace()) {
            counts.clear();
        }
        counts.putAll(payload.counts());
        resort();
    }

    public static void clear() {
        counts.clear();
        sorted = List.of();
    }

    public static List<Map.Entry<String, Integer>> sorted() {
        return sorted;
    }

    private static void resort() {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(counts.size());
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > 0) {
                list.add(Map.entry(entry.getKey(), entry.getValue()));
            }
        }
        list.sort(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()));
        sorted = List.copyOf(list);
    }
}
