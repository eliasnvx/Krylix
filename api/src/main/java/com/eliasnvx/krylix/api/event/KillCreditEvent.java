package com.eliasnvx.krylix.api.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Decides who gets the kill for a death, before {@link KillEvent}. Server thread. Posted for the deaths Krylix handles:
 * players, and hostile mobs (a mob death then only counts if the credited killer is a player).
 *
 * <p>Krylix's own answer is already filled in: the attacker, else whoever knocked the victim off a ledge or into lava
 * (vanilla's kill credit), with a tamed pet's kill credited to its owner. Change it for your own mechanics, for
 * example a turret block's kill to the player who placed it, or a summoned minion's to its summoner.
 */
public final class KillCreditEvent implements KrylixEvent {
    private final LivingEntity victim;
    private final DamageSource source;
    private @Nullable LivingEntity killer;

    public KillCreditEvent(LivingEntity victim, DamageSource source, @Nullable LivingEntity killer) {
        this.victim = victim;
        this.source = source;
        this.killer = killer;
    }

    /** @return the entity that died */
    public LivingEntity victim() {
        return victim;
    }

    /** @return what killed it */
    public DamageSource source() {
        return source;
    }

    /** @return the entity credited with the kill, or {@code null} for a death without a killer */
    public @Nullable LivingEntity killer() {
        return killer;
    }

    /**
     * Credits the kill to someone else. The feed shows the damage cause as the weapon when the killer is not the
     * entity that dealt the blow.
     *
     * @param killer the new killer, or {@code null} for no killer; the victim itself counts as no killer
     */
    public void setKiller(@Nullable LivingEntity killer) {
        this.killer = killer;
    }
}
