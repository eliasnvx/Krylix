package com.eliasnvx.krylix.core;

import java.util.Locale;

public enum HealthBarStyle {
    BLOCKS,
    ASCII,
    DOTS,
    NUMBER_ONLY;

    @Override
    public String toString() {
        return "krylix.health_bar_style." + name().toLowerCase(Locale.ROOT);
    }
}
