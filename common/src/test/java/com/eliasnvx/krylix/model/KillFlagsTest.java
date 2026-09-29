package com.eliasnvx.krylix.model;

import com.eliasnvx.krylix.api.KillFlag;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KillFlagsTest {
    @Test
    void everySetOfFlagsSurvivesTheWire() {
        for (int bits = 0; bits < 1 << KillFlag.values().length; bits++) {
            EnumSet<KillFlag> flags = EnumSet.noneOf(KillFlag.class);
            for (KillFlag flag : KillFlag.values()) {
                if ((bits & (1 << flag.ordinal())) != 0) {
                    flags.add(flag);
                }
            }
            assertEquals(flags, KillFlags.fromBits(KillFlags.toBits(flags)));
        }
        assertEquals(Set.of(), KillFlags.fromBits(0));
    }
}
