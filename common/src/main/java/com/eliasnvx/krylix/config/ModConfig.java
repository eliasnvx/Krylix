package com.eliasnvx.krylix.config;

import com.eliasnvx.krylix.Krylix;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = Krylix.MOD_ID)
public class ModConfig implements ConfigData {
    @ConfigEntry.Gui.CollapsibleObject
    public KrylixConfigData config = new KrylixConfigData();

    /** Hand-edited files: out-of-range numbers are clamped instead of breaking the HUD. */
    @Override
    public void validatePostLoad() {
        if (config == null) {
            config = new KrylixConfigData();
        }
        config.displaySeconds = Math.clamp(config.displaySeconds, 1, 300);
        config.maxEntries = Math.clamp(config.maxEntries, 1, 20);
        config.mobStatsMaxEntries = Math.clamp(config.mobStatsMaxEntries, 1, 50);
        if (config.healthBarStyle == null) {
            config.healthBarStyle = HealthBarStyle.HEARTS;
        }
    }
}
