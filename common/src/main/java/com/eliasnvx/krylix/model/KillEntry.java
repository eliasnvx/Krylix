package com.eliasnvx.krylix.model;

import com.eliasnvx.krylix.network.KrylixPayloads;
import com.eliasnvx.krylix.network.KrylixPayloads.Combatant;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A kill feed row as the client keeps it: the server's data plus the moment it arrived (client clock, so fading
 * does not depend on the server's clock).
 */
public record KillEntry(
    @Nullable Combatant killer,
    Combatant victim,
    float killerHealth,
    String weapon,
    float distance,
    int flags,
    long receivedAt
) {
    private static final double FADE_SECONDS = 1.0;

    public static KillEntry of(KrylixPayloads.KillFeedPayload payload, long now) {
        return new KillEntry(payload.killer(), payload.victim(), payload.killerHealth(), payload.weapon(),
            payload.distance(), payload.flags(), now);
    }

    public boolean isEnvironmentalDeath() {
        return killer == null;
    }

    public boolean isSuicide() {
        if (killer == null) {
            return false;
        }
        if (killer.uuid() != null || victim.uuid() != null) {
            return Objects.equals(killer.uuid(), victim.uuid());
        }
        return killer.name().equals(victim.name()) && Objects.equals(killer.entityType(), victim.entityType());
    }

    public boolean has(int flag) {
        return (flags & flag) != 0;
    }

    public boolean hasDistance() {
        return distance >= 0;
    }

    public boolean isExpired(int displaySeconds, long now) {
        return ageSeconds(now) > displaySeconds;
    }

    /** Fully opaque, then fades out over the last second. */
    public float alpha(int displaySeconds, long now) {
        double age = ageSeconds(now);
        double fadeStart = displaySeconds - FADE_SECONDS;
        if (age < fadeStart) {
            return 1.0f;
        }
        if (age >= displaySeconds) {
            return 0.0f;
        }
        return (float) Math.max(0.0, Math.min(1.0, 1.0 - (age - fadeStart) / FADE_SECONDS));
    }

    private double ageSeconds(long now) {
        return (now - receivedAt) / 1000.0;
    }
}
