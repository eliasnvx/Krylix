package com.eliasnvx.krylix.fabric.client;

import com.eliasnvx.krylix.Krylix;
import com.eliasnvx.krylix.fabric.network.FabricNetworkPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class KrylixFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Krylix.LOGGER.info("Initializing Krylix Fabric Client");

        KrylixKeyBindings.register();
        KrylixClientCommands.register();

        ClientPlayNetworking.registerGlobalReceiver(
            FabricNetworkPackets.KillNotificationPacket.TYPE,
            (payload, context) -> context.client().execute(payload::handleOnClient)
        );

        ClientPlayNetworking.registerGlobalReceiver(
            FabricNetworkPackets.MobStatsSyncPacket.TYPE,
            (payload, context) -> context.client().execute(payload::handleOnClient)
        );

        ClientPlayNetworking.registerGlobalReceiver(
            FabricNetworkPackets.PlayerStatsSyncPacket.TYPE,
            (payload, context) -> context.client().execute(payload::handleOnClient)
        );

        ClientPlayNetworking.registerGlobalReceiver(
            FabricNetworkPackets.DeathRecapPacket.TYPE,
            (payload, context) -> context.client().execute(payload::handleOnClient)
        );

        HudRenderCallback.EVENT.register((guiGraphics, tickCounter) -> {
            float partialTick = tickCounter.getGameTimeDeltaPartialTick(false);
            KillFeedHud.render(guiGraphics, partialTick);
            MobStatsHud.render(guiGraphics, partialTick);
        });

        WorldRenderEvents.AFTER_ENTITIES.register(HealthIndicator::onRenderWorld);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (KrylixKeyBindings.toggleKillFeed.consumeClick()) {
                boolean newState = !KillFeedHud.isHudEnabled();
                KillFeedHud.setEnabled(newState);
                sendToggleMessage(client, "krylix.toggle.killfeed", newState);
            }

            while (KrylixKeyBindings.toggleMobStats.consumeClick()) {
                boolean newState = !MobStatsHud.isStatsEnabled();
                MobStatsHud.setEnabled(newState);
                sendToggleMessage(client, "krylix.toggle.mobstats", newState);
            }

            while (KrylixKeyBindings.openLeaderboard.consumeClick()) {
                client.setScreen(new LeaderboardScreen());
            }

            while (KrylixKeyBindings.toggleHealthIndicator.consumeClick()) {
                boolean newState = !HealthIndicator.isIndicatorEnabled();
                HealthIndicator.setEnabled(newState);
                sendToggleMessage(client, "krylix.toggle.health_indicator", newState);
            }
        });

        Krylix.LOGGER.info("Krylix Fabric Client initialized successfully");
    }

    private void sendToggleMessage(Minecraft client, String translationKey, boolean state) {
        if (client.player != null) {
            Component stateComp = Component.translatable(state ? "krylix.toggle.on" : "krylix.toggle.off");
            client.player.displayClientMessage(
                Component.translatable(translationKey, stateComp),
                true
            );
        }
    }
}
