package com.eliasnvx.krylix.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;

/** config/krylix.json5, read on both sides (the server uses the broadcast setting). */
public final class KrylixConfig {
    private KrylixConfig() {
    }

    /** Cloth already falls back to defaults for a malformed file; a failure here is a real bug, so it is not hidden. */
    public static void init() {
        AutoConfig.register(ModConfig.class, JanksonConfigSerializer::new);
    }

    public static KrylixConfigData get() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().config;
    }

    public static void save() {
        AutoConfig.getConfigHolder(ModConfig.class).save();
    }
}
