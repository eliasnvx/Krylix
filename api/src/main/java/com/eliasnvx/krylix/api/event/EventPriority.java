package com.eliasnvx.krylix.api.event;

/** Order in which listeners are called: {@link #HIGHEST} first. */
public enum EventPriority {
    HIGHEST,
    HIGH,
    NORMAL,
    LOW,
    LOWEST
}
