package com.eliasnvx.krylix.forge.client;

import com.eliasnvx.krylix.forge.config.KrylixConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;

public class MobStatsHud {
    private static final int iconSize = 12;
    private static final int rowHeight = 14;
    private static final int padding = 4;
    private static final int iconCornerCut = 2;

    public static void setEnabled(boolean enabled) {
        KrylixConfig.get().mobStatsEnabled = enabled;
        com.eliasnvx.krylix.forge.config.KrylixConfig.save();
    }

    public static boolean isStatsEnabled() {
        return KrylixConfig.get().mobStatsEnabled;
    }

    public static void render(GuiGraphicsExtractor guiGraphics, float partialTick) {
        if (!KrylixConfig.get().mobStatsEnabled) return;

        List<Map.Entry<String, Integer>> topEntries = MobStatsClient.sortedByCount();
        int max = Math.min(topEntries.size(), KrylixConfig.get().mobStatsMaxEntries);
        if (max == 0) return;
        topEntries = topEntries.subList(0, max);

        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;

        int x = 5;
        int y = 5;
        for (Map.Entry<String, Integer> entry : topEntries) {
            String entityId = entry.getKey();
            int count = entry.getValue();
            Identifier texture = MobTextures.byEntityId(entityId);
            
            final int finalY = y;
            if (texture != null) {
                MobTextures.blitMobFace(guiGraphics, texture, entityId, x, finalY, iconSize);
            } else {
                HudRender.roundedFill(guiGraphics, x, finalY, iconSize, iconSize, iconCornerCut, 0x80808080);
            }
            
            guiGraphics.text(font, MobStatsClient.displayName(entityId) + ": " + count, x + iconSize + padding, finalY + 2, 0xFFFFFFFF);
            y += rowHeight;
        }
    }
}
