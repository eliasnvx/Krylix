package com.eliasnvx.krylix.server;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Damage players dealt to each target in the last {@link #WINDOW_MS}, for the death recap's "damage dealt".
 * Server thread only. Old entries are swept regularly, so the log stays as small as the fights going on.
 */
public final class CombatLog {
    static final long WINDOW_MS = 20_000;
    private static final long SWEEP_EVERY_MS = 10_000;

    private record Key(UUID attacker, UUID target) {
    }

    private record Total(float damage, long lastHit) {
    }

    private final Map<Key, Total> totals = new HashMap<>();
    private long lastSweep;

    public void record(UUID attacker, UUID target, float damage, long now) {
        totals.merge(new Key(attacker, target), new Total(damage, now), (old, add) ->
            now - old.lastHit() < WINDOW_MS ? new Total(old.damage() + add.damage(), now) : add);
        if (now - lastSweep > SWEEP_EVERY_MS) {
            sweep(now);
        }
    }

    /** What {@code attacker} dealt to {@code target} in the current fight, or 0. */
    public float dealt(UUID attacker, UUID target, long now) {
        Total total = totals.get(new Key(attacker, target));
        return total != null && now - total.lastHit() < WINDOW_MS ? total.damage() : 0f;
    }

    /** Forget a player entirely (they left). */
    public void forget(UUID player) {
        totals.keySet().removeIf(k -> k.attacker().equals(player) || k.target().equals(player));
    }

    private void sweep(long now) {
        lastSweep = now;
        for (Iterator<Total> it = totals.values().iterator(); it.hasNext(); ) {
            if (now - it.next().lastHit() >= WINDOW_MS) {
                it.remove();
            }
        }
    }

    int size() {
        return totals.size();
    }
}
