package com.eliasnvx.krylix.forge.config;

import com.eliasnvx.krylix.core.KrylixConfigData;
import me.shedaniel.autoconfig.AutoConfig;

public class KrylixConfig {
    public static KrylixConfigData get() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().config;
    }

    public static void save() {
        AutoConfig.getConfigHolder(ModConfig.class).save();
    }
}
