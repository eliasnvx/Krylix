package com.eliasnvx.krylix.server;

import com.eliasnvx.krylix.network.KrylixPayloads.LeaderboardRow;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardRowsTest {
    @Test
    void keepsTheTopOfBothTabsWithoutDuplicates() {
        PlayerKillStatsData data = new PlayerKillStatsData();
        for (int i = 0; i < 100; i++) {
            PlayerStat stat = new PlayerStat("p" + i);
            stat.kills = i;             // p99 has the most PvP kills
            stat.mobKills = 1000 - i;   // p0 has the most mob kills
            data.stats.put(new UUID(0, i).toString(), stat);
        }
        data.stats.put("not-a-uuid", new PlayerStat("broken"));

        List<LeaderboardRow> all = data.leaderboardRows(1000);
        assertEquals(100, all.size(), "the bad key is skipped");

        List<LeaderboardRow> rows = data.leaderboardRows(10);
        assertEquals(10, rows.size());
        assertEquals(10, new HashSet<>(rows.stream().map(LeaderboardRow::uuid).toList()).size(), "no duplicates");
        List<String> names = rows.stream().map(LeaderboardRow::name).toList();
        for (String top : List.of("p99", "p98", "p97", "p96", "p95", "p0", "p1", "p2", "p3", "p4")) {
            assertTrue(names.contains(top), top + " missing from " + names);
        }
        assertEquals("p99", names.getFirst(), "PvP leaders first");
    }

    @Test
    void versionChangesWithEveryRecord() {
        PlayerKillStatsData data = new PlayerKillStatsData();
        long v0 = data.version();
        UUID id = UUID.randomUUID();
        data.recordKill(id, "a");
        data.recordMobKill(id, "a", "minecraft:zombie");
        assertEquals(v0 + 2, data.version());
        data.updateName(id, "a");
        assertEquals(v0 + 2, data.version(), "same name: no change");
        data.updateName(id, "b");
        assertEquals(v0 + 3, data.version());
    }
}
