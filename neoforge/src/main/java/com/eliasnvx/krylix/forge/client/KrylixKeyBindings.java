package com.eliasnvx.krylix.forge.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
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

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(toggleKillFeed);
        event.register(toggleMobStats);
        event.register(openLeaderboard);
        event.register(toggleHealthIndicator);
    }
}
