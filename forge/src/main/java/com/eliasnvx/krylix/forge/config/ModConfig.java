package com.eliasnvx.krylix.forge.config;

import com.eliasnvx.krylix.core.KrylixConfigData;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "krylix")
public class ModConfig implements ConfigData {
    @ConfigEntry.Gui.CollapsibleObject
    public KrylixConfigData config = new KrylixConfigData();
}
