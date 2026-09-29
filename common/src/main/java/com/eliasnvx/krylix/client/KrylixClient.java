package com.eliasnvx.krylix.client;

import com.eliasnvx.krylix.model.KillEntry;
import com.eliasnvx.krylix.network.KrylixPayloads.DeathRecapPayload;
import com.eliasnvx.krylix.network.KrylixPayloads.KillFeedPayload;
import com.eliasnvx.krylix.network.KrylixPayloads.LeaderboardPayload;
import com.eliasnvx.krylix.network.KrylixPayloads.MobKillsPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundEvents;

/** Client-side Krylix: the loaders forward their events here. Client thread only. */
public final class KrylixClient {
    private KrylixClient() {
    }

    /** End of every client tick. */
    public static void onTick(Minecraft minecraft) {
        while (KrylixKeyBindings.TOGGLE_KILL_FEED.consumeClick()) {
            boolean enabled = !KillFeedHud.isHudEnabled();
            KillFeedHud.setEnabled(enabled);
            notifyToggle(minecraft, "krylix.toggle.killfeed", enabled);
        }
        while (KrylixKeyBindings.TOGGLE_MOB_STATS.consumeClick()) {
            boolean enabled = !MobStatsHud.isStatsEnabled();
            MobStatsHud.setEnabled(enabled);
            notifyToggle(minecraft, "krylix.toggle.mobstats", enabled);
        }
        while (KrylixKeyBindings.OPEN_LEADERBOARD.consumeClick()) {
            if (minecraft.gui.screen() == null) {
                minecraft.gui.setScreen(new LeaderboardScreen());
            }
        }
        while (KrylixKeyBindings.TOGGLE_HEALTH_PLATES.consumeClick()) {
            boolean enabled = !HealthIndicator.isIndicatorEnabled();
            HealthIndicator.setEnabled(enabled);
            notifyToggle(minecraft, "krylix.toggle.health_indicator", enabled);
        }
        HealthIndicator.updateTarget();
        if (DeathRecapClient.hasRecap() && minecraft.player != null && minecraft.player.isAlive()
            && !(minecraft.gui.screen() instanceof DeathScreen)) {
            DeathRecapClient.clear(); // respawned
        }
    }

    /** The kill feed and the mob panel, drawn with the HUD. */
    public static void renderHud(GuiGraphicsExtractor graphics) {
        Screen screen = Minecraft.getInstance().gui.screen();
        if (screen != null && !(screen instanceof ChatScreen)) {
            return; // don't draw over inventories and menus; chat is fine
        }
        KillFeedHud.render(graphics);
        MobStatsHud.render(graphics);
    }

    /** After the death screen drew itself. */
    public static void renderDeathScreen(GuiGraphicsExtractor graphics, int width, int height) {
        DeathRecapClient.render(graphics, width, height);
    }

    /** Left a world or server: nothing from it should show up in the next one. */
    public static void onDisconnect() {
        KillFeedHud.clear();
        MobStatsClient.clear();
        LeaderboardClient.clear();
        DeathRecapClient.clear();
        HealthIndicator.reset();
    }

    /** A Krylix payload from the server, on the client thread. */
    public static void handle(CustomPacketPayload payload) {
        switch (payload) {
            case KillFeedPayload p -> KillFeedHud.add(KillEntry.of(p, System.currentTimeMillis()));
            case DeathRecapPayload p -> DeathRecapClient.set(p);
            case MobKillsPayload p -> MobStatsClient.apply(p);
            case LeaderboardPayload p -> LeaderboardClient.apply(p);
            default -> {
            }
        }
    }

    /** A click and an action-bar line, so a key press is never silent. */
    private static void notifyToggle(Minecraft minecraft, String key, boolean enabled) {
        if (minecraft.player != null) {
            Component state = Component.translatable(enabled ? "krylix.toggle.on" : "krylix.toggle.off");
            minecraft.player.sendOverlayMessage(Component.translatable(key, state));
        }
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }
}
