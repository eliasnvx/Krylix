package com.eliasnvx.krylix.model;

import com.eliasnvx.krylix.api.KillFlag;
import com.eliasnvx.krylix.network.KrylixPayloads;

import java.util.EnumSet;
import java.util.Set;

/** {@link KillFlag}s ↔ the bit field in the payloads. */
public final class KillFlags {
    private KillFlags() {
    }

    public static EnumSet<KillFlag> fromBits(int bits) {
        EnumSet<KillFlag> flags = EnumSet.noneOf(KillFlag.class);
        if ((bits & KrylixPayloads.FLAG_CRITICAL) != 0) flags.add(KillFlag.CRITICAL);
        if ((bits & KrylixPayloads.FLAG_SMASH) != 0) flags.add(KillFlag.SMASH);
        if ((bits & KrylixPayloads.FLAG_LONGSHOT) != 0) flags.add(KillFlag.LONGSHOT);
        return flags;
    }

    public static int toBits(Set<KillFlag> flags) {
        int bits = 0;
        if (flags.contains(KillFlag.CRITICAL)) bits |= KrylixPayloads.FLAG_CRITICAL;
        if (flags.contains(KillFlag.SMASH)) bits |= KrylixPayloads.FLAG_SMASH;
        if (flags.contains(KillFlag.LONGSHOT)) bits |= KrylixPayloads.FLAG_LONGSHOT;
        return bits;
    }
}
