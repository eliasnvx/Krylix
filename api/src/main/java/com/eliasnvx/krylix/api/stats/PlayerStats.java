package com.eliasnvx.krylix.api.stats;

import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.UUID;

/**
 * A snapshot of one player's all-time statistics.
 *
 * @param uuid           the player
 * @param name           the name they last played under
 * @param kills          players killed
 * @param deaths         deaths, from any cause
 * @param mobKills       hostile mobs killed
 * @param mobKillsByType hostile mobs killed, by entity type (immutable)
 */
public record PlayerStats(UUID uuid, String name, int kills, int deaths, int mobKills, Map<Identifier, Integer> mobKillsByType) {
    public PlayerStats {
        mobKillsByType = Map.copyOf(mobKillsByType);
    }

    /** @return kills per death; the kill count when there are no deaths */
    public double killDeathRatio() {
        return deaths == 0 ? kills : (double) kills / deaths;
    }
}
