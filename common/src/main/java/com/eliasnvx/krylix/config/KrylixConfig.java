package com.eliasnvx.krylix.config;

import com.eliasnvx.krylix.Krylix;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;

/** config/krylix.json5, read on both sides (the server uses the broadcast setting). */
public final class KrylixConfig {
    private KrylixConfig() {
    }

    public static void init() {
        try {
            AutoConfig.register(ModConfig.class, JanksonConfigSerializer::new);
        } catch (RuntimeException e) {
            Krylix.LOGGER.error("Could not load the Krylix config, using defaults", e);
        }
    }

    public static KrylixConfigData get() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().config;
    }

    public static void save() {
        AutoConfig.getConfigHolder(ModConfig.class).save();
    }
}
