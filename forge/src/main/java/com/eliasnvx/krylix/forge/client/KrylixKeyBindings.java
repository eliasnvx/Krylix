package com.eliasnvx.krylix.forge.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

public class KrylixKeyBindings {
    private static final String CATEGORY = "key.categories.krylix";

    public static final KeyMapping TOGGLE_KILLFEED = new KeyMapping(
        "key.krylix.toggle_killfeed",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_K,
        CATEGORY
    );

    public static final KeyMapping TOGGLE_MOBSTATS = new KeyMapping(
        "key.krylix.toggle_mobstats",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_L,
        CATEGORY
    );

    public static final KeyMapping OPEN_LEADERBOARD = new KeyMapping(
        "key.krylix.open_leaderboard",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_O,
        CATEGORY
    );

    public static final KeyMapping TOGGLE_HEALTH_INDICATOR = new KeyMapping(
        "key.krylix.toggle_health_indicator",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_H,
        CATEGORY
    );

    public static void register() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(KrylixKeyBindings::onRegisterKeyMappings);
    }

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_KILLFEED);
        event.register(TOGGLE_MOBSTATS);
        event.register(OPEN_LEADERBOARD);
        event.register(TOGGLE_HEALTH_INDICATOR);
    }

    public static void handleInput() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        while (TOGGLE_KILLFEED.consumeClick()) {
            boolean current = KillFeedHud.isHudEnabled();
            KillFeedHud.setEnabled(!current);
            showToggleStatus("krylix.toggle.killfeed", !current);
        }

        while (TOGGLE_MOBSTATS.consumeClick()) {
            boolean current = MobStatsHud.isStatsEnabled();
            MobStatsHud.setEnabled(!current);
            showToggleStatus("krylix.toggle.mobstats", !current);
        }

        while (OPEN_LEADERBOARD.consumeClick()) {
            minecraft.setScreen(new LeaderboardScreen());
        }

        while (TOGGLE_HEALTH_INDICATOR.consumeClick()) {
            boolean current = HealthIndicator.isEnabled();
            HealthIndicator.setEnabled(!current);
            showToggleStatus("krylix.toggle.health_indicator", !current);
        }
    }

    private static void showToggleStatus(String key, boolean enabled) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        Component status = Component.translatable(enabled ? "krylix.toggle.on" : "krylix.toggle.off");
        minecraft.player.displayClientMessage(Component.translatable(key, status), true);
    }
}
