package com.eliasnvx.krylix.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class KrylixPayloadsTest {
    @Test
    void clipsLongStringsWithoutSplittingSurrogates() {
        String shortName = "Zombie";
        assertSame(shortName, KrylixPayloads.clip(shortName, 256));
        assertEquals(256, KrylixPayloads.clip("x".repeat(300), 256).length());
        String emoji = "a".repeat(255) + "🔥"; // 255 chars + a surrogate pair
        assertEquals(255, KrylixPayloads.clip(emoji, 256).length());
    }
}
