package com.eliasnvx.krylix.fabric.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class KrylixKeyBindings {
    public static final KeyMapping toggleKillFeed = new KeyMapping(
        "key.krylix.toggle_killfeed",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_K,
        "key.categories.krylix"
    );

    public static final KeyMapping toggleMobStats = new KeyMapping(
        "key.krylix.toggle_mobstats",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_L,
        "key.categories.krylix"
    );

    public static final KeyMapping openLeaderboard = new KeyMapping(
        "key.krylix.open_leaderboard",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_O,
        "key.categories.krylix"
    );

    public static final KeyMapping toggleHealthIndicator = new KeyMapping(
        "key.krylix.toggle_health_indicator",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_H,
        "key.categories.krylix"
    );

    public static void register() {
        KeyBindingHelper.registerKeyBinding(toggleKillFeed);
        KeyBindingHelper.registerKeyBinding(toggleMobStats);
        KeyBindingHelper.registerKeyBinding(openLeaderboard);
        KeyBindingHelper.registerKeyBinding(toggleHealthIndicator);
    }
}
