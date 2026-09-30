package com.eliasnvx.krylix.server;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Damage players dealt to each target in the last {@link #WINDOW_TICKS}, for the death recap's "damage dealt".
 * Server thread only. Times are server ticks, so a paused singleplayer game or a lagging server doesn't end a fight.
 * Old entries are swept regularly, so the log stays as small as the fights going on.
 */
public final class CombatLog {
    static final int WINDOW_TICKS = 20 * 20;
    private static final int SWEEP_EVERY_TICKS = 20 * 10;

    private record Key(UUID attacker, UUID target) {
    }

    private record Total(float damage, int lastHit) {
    }

    private final Map<Key, Total> totals = new HashMap<>();
    private int lastSweep;

    public void record(UUID attacker, UUID target, float damage, int tick) {
        totals.merge(new Key(attacker, target), new Total(damage, tick), (old, add) ->
            tick - old.lastHit() < WINDOW_TICKS ? new Total(old.damage() + add.damage(), tick) : add);
        if (tick - lastSweep > SWEEP_EVERY_TICKS || tick < lastSweep) {
            sweep(tick);
        }
    }

    /** What {@code attacker} dealt to {@code target} in the current fight, or 0. */
    public float dealt(UUID attacker, UUID target, int tick) {
        Total total = totals.get(new Key(attacker, target));
        return total != null && tick - total.lastHit() < WINDOW_TICKS ? total.damage() : 0f;
    }

    /** Forget a player entirely (they left). */
    public void forget(UUID player) {
        totals.keySet().removeIf(k -> k.attacker().equals(player) || k.target().equals(player));
    }

    public void clear() {
        totals.clear();
        lastSweep = 0;
    }

    private void sweep(int tick) {
        lastSweep = tick;
        for (Iterator<Total> it = totals.values().iterator(); it.hasNext(); ) {
            int age = tick - it.next().lastHit();
            if (age >= WINDOW_TICKS || age < 0) {
                it.remove();
            }
        }
    }

    int size() {
        return totals.size();
    }
}
