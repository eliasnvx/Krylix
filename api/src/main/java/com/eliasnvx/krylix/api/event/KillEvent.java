package com.eliasnvx.krylix.api.event;

import com.eliasnvx.krylix.api.KillFlag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

/**
 * A death Krylix is about to handle: a player died, or a player killed a hostile mob. Server thread.
 *
 * <p>For a player's death Krylix sends the kill feed row, the victim's death recap and counts the kill and the death;
 * for a mob kill it counts the mob in the killer's statistics. Cancel to make Krylix ignore the death entirely, or
 * switch off only the {@linkplain #setBroadcast feed row} or only the {@linkplain #setRecordStats statistics}
 * (a practice arena, a minigame). The weapon and the flags can be changed; they are what players see.
 */
public final class KillEvent implements CancellableEvent {
    private final LivingEntity victim;
    private final @Nullable LivingEntity killer;
    private final DamageSource source;
    private final float distance;
    private final EnumSet<KillFlag> flags;
    private Identifier weapon;
    private boolean broadcast = true;
    private boolean recordStats = true;
    private boolean cancelled;

    public KillEvent(LivingEntity victim, @Nullable LivingEntity killer, DamageSource source, Identifier weapon,
                     float distance, Set<KillFlag> flags) {
        this.victim = victim;
        this.killer = killer;
        this.source = source;
        this.weapon = weapon;
        this.distance = distance;
        this.flags = flags.isEmpty() ? EnumSet.noneOf(KillFlag.class) : EnumSet.copyOf(flags);
    }

    /** @return the entity that died */
    public LivingEntity victim() {
        return victim;
    }

    /** @return the credited killer (see {@link KillCreditEvent}), or {@code null} for a death without one */
    public @Nullable LivingEntity killer() {
        return killer;
    }

    /** @return what killed the victim */
    public DamageSource source() {
        return source;
    }

    /**
     * The item shown between the two names: the weapon, or an item standing for the cause (a feather for a fall).
     * {@code minecraft:air} shows a plain arrow.
     *
     * @return an item id
     */
    public Identifier weapon() {
        return weapon;
    }

    /** @param weapon an item id; unknown items show the plain arrow */
    public void setWeapon(Identifier weapon) {
        this.weapon = weapon;
    }

    /** @return killer-to-victim distance in blocks, or -1 without a killer */
    public float distance() {
        return distance;
    }

    /** @return the kill's flags; add or remove to change the badge */
    public Set<KillFlag> flags() {
        return flags;
    }

    /** @return whether players get a kill feed row (player deaths only) */
    public boolean broadcast() {
        return broadcast;
    }

    /** @param broadcast whether players get a kill feed row */
    public void setBroadcast(boolean broadcast) {
        this.broadcast = broadcast;
    }

    /** @return whether the kill and death are counted in the statistics */
    public boolean recordStats() {
        return recordStats;
    }

    /** @param recordStats whether the kill and death are counted in the statistics */
    public void setRecordStats(boolean recordStats) {
        this.recordStats = recordStats;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void cancel() {
        cancelled = true;
    }
}
