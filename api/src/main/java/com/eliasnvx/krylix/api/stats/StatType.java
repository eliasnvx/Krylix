package com.eliasnvx.krylix.api.stats;

/** The statistics Krylix keeps per player. New constants may be added in a minor API version: handle unknown ones. */
public enum StatType {
    /** A player killed another player. */
    KILL,
    /** A player died. */
    DEATH,
    /** A player killed a hostile mob. */
    MOB_KILL
}
