package com.eliasnvx.krylix.forge.client;

import com.eliasnvx.krylix.Krylix;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class KrylixKeyBindings {
    public static final KeyMapping toggleKillFeed = new KeyMapping(
        "key.krylix.toggle_killfeed",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_K),
        "key.categories.krylix"
    );

    public static final KeyMapping toggleMobStats = new KeyMapping(
        "key.krylix.toggle_mobstats",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_L),
        "key.categories.krylix"
    );

    public static final KeyMapping openLeaderboard = new KeyMapping(
        "key.krylix.open_leaderboard",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_O),
        "key.categories.krylix"
    );

    public static final KeyMapping toggleHealthIndicator = new KeyMapping(
        "key.krylix.toggle_health_indicator",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_H),
        "key.categories.krylix"
    );

    @EventBusSubscriber(modid = Krylix.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(toggleKillFeed);
            event.register(toggleMobStats);
            event.register(openLeaderboard);
            event.register(toggleHealthIndicator);
        }
    }
}
