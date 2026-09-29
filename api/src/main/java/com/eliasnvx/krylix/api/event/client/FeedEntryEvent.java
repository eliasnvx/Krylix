package com.eliasnvx.krylix.api.event.client;

import com.eliasnvx.krylix.api.KillFlag;
import com.eliasnvx.krylix.api.event.CancellableEvent;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

/**
 * A kill feed row arrived from the server and is about to be shown. Client thread. Cancel to hide it, for example
 * rows about training dummies, or kills you track in your own HUD.
 */
public final class FeedEntryEvent implements CancellableEvent {
    private final @Nullable Participant killer;
    private final Participant victim;
    private final Identifier weapon;
    private final float distance;
    private final Set<KillFlag> flags;
    private boolean cancelled;

    public FeedEntryEvent(@Nullable Participant killer, Participant victim, Identifier weapon, float distance, Set<KillFlag> flags) {
        this.killer = killer;
        this.victim = victim;
        this.weapon = weapon;
        this.distance = distance;
        this.flags = Set.copyOf(flags);
    }

    /** @return the killer, or {@code null} for a death without one (a fall, lava) */
    public @Nullable Participant killer() {
        return killer;
    }

    /** @return the victim */
    public Participant victim() {
        return victim;
    }

    /** @return the item shown between the names */
    public Identifier weapon() {
        return weapon;
    }

    /** @return killer-to-victim distance in blocks, or -1 without a killer */
    public float distance() {
        return distance;
    }

    /** @return the kill's flags (read-only) */
    public Set<KillFlag> flags() {
        return flags;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void cancel() {
        cancelled = true;
    }

    /**
     * One side of a feed row, as the server described it.
     *
     * @param name       the name shown (a custom name, or the entity type's name in the server's language)
     * @param uuid       set for players, {@code null} for other entities
     * @param entityType the entity type id, when the server sent one
     */
    public record Participant(String name, @Nullable UUID uuid, @Nullable Identifier entityType) {
        /** @return whether this is a player */
        public boolean isPlayer() {
            return uuid != null;
        }
    }
}
