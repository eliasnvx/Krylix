package com.eliasnvx.krylix.fabric.config;

import com.eliasnvx.krylix.Krylix;
import com.eliasnvx.krylix.core.KrylixConfigData;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = Krylix.MOD_ID)
public class ModConfig implements ConfigData {
    @ConfigEntry.Gui.CollapsibleObject
    public KrylixConfigData config = new KrylixConfigData();
}
