package com.eliasnvx.krylix.server;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CombatLogTest {
    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();

    @Test
    void addsUpHitsWithinTheWindowAndForgetsOldFights() {
        CombatLog log = new CombatLog();
        log.record(a, b, 4f, 100);
        log.record(a, b, 3f, 150);
        assertEquals(7f, log.dealt(a, b, 160));
        assertEquals(0f, log.dealt(b, a, 160), "direction matters");
        assertEquals(0f, log.dealt(a, b, 150 + CombatLog.WINDOW_TICKS), "the fight is over");
        log.record(a, b, 2f, 150 + CombatLog.WINDOW_TICKS + 1);
        assertEquals(2f, log.dealt(a, b, 150 + CombatLog.WINDOW_TICKS + 1), "a new fight starts from zero");
    }

    @Test
    void sweepsOldEntriesAndForgetsPlayersWhoLeft() {
        CombatLog log = new CombatLog();
        log.record(a, b, 1f, 0);
        log.record(b, a, 1f, 0);
        log.record(a, UUID.randomUUID(), 1f, 10_000); // triggers a sweep: both old fights are gone
        assertEquals(1, log.size());
        log.forget(a);
        assertEquals(0, log.size());
    }
}
