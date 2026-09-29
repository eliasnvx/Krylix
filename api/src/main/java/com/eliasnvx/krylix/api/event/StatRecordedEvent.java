package com.eliasnvx.krylix.api.event;

import com.eliasnvx.krylix.api.stats.StatType;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A player's statistic went up by one: after it was saved, on the server thread. For rewards, external leaderboards
 * or webhooks. Read-only.
 *
 * @param player     the player's UUID
 * @param playerName the player's name at the time
 * @param type       which statistic
 * @param entityType for {@link StatType#MOB_KILL}, the mob's entity type; otherwise {@code null}
 * @param total      the new total: all PvP kills, all deaths, or kills of this mob type
 */
public record StatRecordedEvent(UUID player, String playerName, StatType type, @Nullable Identifier entityType, int total)
    implements KrylixEvent {
}
