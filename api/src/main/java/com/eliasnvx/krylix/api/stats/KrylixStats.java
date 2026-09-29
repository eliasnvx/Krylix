package com.eliasnvx.krylix.api.stats;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Read-only view of the statistics saved in a world. Server thread only. */
public interface KrylixStats {
    /**
     * @param player a player's UUID
     * @return their statistics, or empty if they never killed or died on this world
     */
    Optional<PlayerStats> get(UUID player);

    /** @return every tracked player, in no particular order */
    List<PlayerStats> all();
}
