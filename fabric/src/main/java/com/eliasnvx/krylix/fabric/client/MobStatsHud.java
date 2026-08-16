package com.eliasnvx.krylix.fabric.client;

import com.eliasnvx.krylix.fabric.config.KrylixConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class MobStatsHud {
    public static boolean isStatsEnabled() {
        return KrylixConfig.get().mobStatsEnabled;
    }

    public static void setEnabled(boolean enabled) {
        KrylixConfig.get().mobStatsEnabled = enabled;
        KrylixConfig.save();
    }

    public static void render(GuiGraphics guiGraphics, float partialTick) {
        if (!isStatsEnabled()) return;

        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        Map<String, Integer> stats = MobStatsClient.getStats();
        if (stats == null || stats.isEmpty()) return;

        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int maxEntries = KrylixConfig.get().mobStatsMaxEntries;

        List<Map.Entry<String, Integer>> sorted = stats.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(maxEntries)
                .toList();

        if (sorted.isEmpty()) return;

        int rowHeight = 16;
        int iconSize = 12;
        int padding = 4;
        int startX = 6;
        int startY = 6;

        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String, Integer> entry = sorted.get(i);
            int y = startY + (i * rowHeight);

            ResourceLocation texture = MobTextures.byEntityId(entry.getKey());
            if (texture != null) {
                MobTextures.blitMobFace(guiGraphics, texture, entry.getKey(), startX, y + 2, iconSize);
            } else {
                guiGraphics.fill(startX, y + 2, startX + iconSize, y + 2 + iconSize, 0x80808080);
            }

            String countStr = String.valueOf(entry.getValue());
            guiGraphics.drawString(font, countStr, startX + iconSize + padding, y + 4, 0xFFFFFF);
        }
    }
}
