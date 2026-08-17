package com.eliasnvx.krylix.fabric.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class KrylixKeyBindings {
    public static final KeyMapping toggleKillFeed = new KeyMapping(
        "key.krylix.toggle_killfeed",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_K,
        KeyMapping.Category.MISC
    );

    public static final KeyMapping toggleMobStats = new KeyMapping(
        "key.krylix.toggle_mobstats",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_L,
        KeyMapping.Category.MISC
    );

    public static final KeyMapping openLeaderboard = new KeyMapping(
        "key.krylix.open_leaderboard",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_O,
        KeyMapping.Category.MISC
    );

    public static final KeyMapping toggleHealthIndicator = new KeyMapping(
        "key.krylix.toggle_health_indicator",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_H,
        KeyMapping.Category.MISC
    );

    public static void register() {
        KeyMappingHelper.registerKeyMapping(toggleKillFeed);
        KeyMappingHelper.registerKeyMapping(toggleMobStats);
        KeyMappingHelper.registerKeyMapping(openLeaderboard);
        KeyMappingHelper.registerKeyMapping(toggleHealthIndicator);
    }
}
