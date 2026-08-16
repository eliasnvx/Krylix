package com.eliasnvx.krylix.forge.client;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class KrylixClient {
    public static void registerClientEvents() {
        MinecraftForge.EVENT_BUS.register(KrylixClient.class);
        MinecraftForge.EVENT_BUS.register(DeathRecapClient.class);
        MinecraftForge.EVENT_BUS.register(HealthIndicator.class);
        KrylixKeyBindings.register();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            KrylixKeyBindings.handleInput();
        }
    }
}
