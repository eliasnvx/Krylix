package com.eliasnvx.krylix.api;

/** What made a kill special. Shown as a badge in the kill feed and on the death recap. */
public enum KillFlag {
    /** A falling melee critical hit, by vanilla's rules. */
    CRITICAL,
    /** A mace smash attack. */
    SMASH,
    /** A projectile kill from 30 blocks or more. */
    LONGSHOT
}
