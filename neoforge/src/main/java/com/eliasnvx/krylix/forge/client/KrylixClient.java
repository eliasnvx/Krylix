package com.eliasnvx.krylix.forge.client;

import com.eliasnvx.krylix.Krylix;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;

public class KrylixClient {

    static {
        System.out.println("KrylixClient loaded!");
        Krylix.LOGGER.info("KrylixClient class initialized successfully");
    }

    public static void registerClientEvents() {
        System.out.println("KrylixClient: Registering client events manually!");
        Krylix.LOGGER.info("Registering Krylix client events manually");

        KrylixClient instance = new KrylixClient();
        NeoForge.EVENT_BUS.addListener(instance::onRenderGui);
        NeoForge.EVENT_BUS.addListener(instance::onClientTick);
        NeoForge.EVENT_BUS.addListener(HealthIndicator::onRenderLevelStage);
        NeoForge.EVENT_BUS.addListener(DeathRecapClient::onScreenRender);

        Krylix.LOGGER.info("Krylix client events registered successfully");
    }

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) return;

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        KillFeedHud.render(event.getGuiGraphics(), partialTick);
        MobStatsHud.render(event.getGuiGraphics(), partialTick);
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        while (KrylixKeyBindings.toggleKillFeed.consumeClick()) {
            boolean enabled = !KillFeedHud.isHudEnabled();
            KillFeedHud.setEnabled(enabled);
            notifyToggle("krylix.toggle.killfeed", enabled);
        }
        while (KrylixKeyBindings.toggleMobStats.consumeClick()) {
            boolean enabled = !MobStatsHud.isStatsEnabled();
            MobStatsHud.setEnabled(enabled);
            notifyToggle("krylix.toggle.mobstats", enabled);
        }
        while (KrylixKeyBindings.openLeaderboard.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen == null) {
                minecraft.setScreen(new LeaderboardScreen());
            }
        }
        while (KrylixKeyBindings.toggleHealthIndicator.consumeClick()) {
            boolean enabled = !HealthIndicator.isIndicatorEnabled();
            HealthIndicator.setEnabled(enabled);
            notifyToggle("krylix.toggle.health_indicator", enabled);
        }

        HealthIndicator.updateTarget();
    }

    private void notifyToggle(String translationKey, boolean enabled) {
        Minecraft minecraft = Minecraft.getInstance();
        Component stateText = Component.translatable(enabled ? "krylix.toggle.on" : "krylix.toggle.off");
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable(translationKey, stateText), true);
        }
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }
}
