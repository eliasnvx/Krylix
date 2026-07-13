package com.eliasnvx.krylix.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KillEntryTest {

    private fun entryAged(ageMillis: Long, killerName: String? = "Killer", victimName: String = "Victim"): KillEntry =
        KillEntry(
            killerName = killerName,
            victimName = victimName,
            weaponName = "minecraft:diamond_sword",
            timestamp = System.currentTimeMillis() - ageMillis
        )

    @Test
    fun `isExpired is false for a fresh entry`() {
        assertFalse(entryAged(0).isExpired(15))
    }

    @Test
    fun `isExpired is false right before the fade window ends`() {
        assertFalse(entryAged(14_000).isExpired(15))
    }

    @Test
    fun `isExpired is true after the fade window`() {
        assertTrue(entryAged(20_000).isExpired(15))
    }

    @Test
    fun `getAlpha is fully opaque for a fresh entry`() {
        assertEquals(1.0f, entryAged(0).getAlpha(15))
    }

    @Test
    fun `getAlpha is fully opaque before the fade-out starts`() {
        // fade starts 1s before fadeTime, so at fadeTime - 2s we're still before it
        assertEquals(1.0f, entryAged(13_000).getAlpha(15))
    }

    @Test
    fun `getAlpha decreases during the last second before expiry`() {
        // halfway through the 1s fade-out window (fadeTime - 0.5s)
        val alpha = entryAged(14_500).getAlpha(15)
        assertTrue(alpha in 0.0f..1.0f, "expected alpha in [0,1], was $alpha")
        assertTrue(alpha < 1.0f, "expected alpha to have started fading, was $alpha")
    }

    @Test
    fun `getAlpha is zero once expired`() {
        assertEquals(0.0f, entryAged(20_000).getAlpha(15))
    }

    @Test
    fun `isEnvironmentalDeath is true when there is no killer`() {
        assertTrue(entryAged(0, killerName = null).isEnvironmentalDeath)
    }

    @Test
    fun `isEnvironmentalDeath is false when there is a killer`() {
        assertFalse(entryAged(0).isEnvironmentalDeath)
    }

    @Test
    fun `isSuicide is true when killer and victim match`() {
        assertTrue(entryAged(0, killerName = "Steve", victimName = "Steve").isSuicide)
    }

    @Test
    fun `isSuicide is false when killer and victim differ`() {
        assertFalse(entryAged(0, killerName = "Steve", victimName = "Alex").isSuicide)
    }
}
