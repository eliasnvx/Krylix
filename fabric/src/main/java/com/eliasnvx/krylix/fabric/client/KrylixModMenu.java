package com.eliasnvx.krylix.fabric.client;

import com.eliasnvx.krylix.config.ModConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfigClient;

/** The Config button in Mod Menu. Loaded by Mod Menu only, so Krylix runs fine without it. */
public class KrylixModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> AutoConfigClient.getConfigScreen(ModConfig.class, parent).get();
    }
}
