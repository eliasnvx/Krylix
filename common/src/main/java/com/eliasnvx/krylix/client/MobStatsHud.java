package com.eliasnvx.krylix.client;

import com.eliasnvx.krylix.config.KrylixConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;
import java.util.Map;

/** Top-left panel: the hostile mobs this player has killed in this world, most first. */
public final class MobStatsHud {
    private static final int ROW = 16;
    private static final int ICON = 12;
    private static final int PAD = 4;
    private static final int X = 6;
    private static final int Y = 6;

    private MobStatsHud() {
    }

    public static boolean isStatsEnabled() {
        return KrylixConfig.get().mobStatsEnabled;
    }

    public static void setEnabled(boolean enabled) {
        KrylixConfig.get().mobStatsEnabled = enabled;
        KrylixConfig.save();
    }

    public static void render(GuiGraphicsExtractor graphics) {
        if (!isStatsEnabled()) {
            return;
        }
        List<Map.Entry<String, Integer>> sorted = MobStatsClient.sorted();
        if (sorted.isEmpty()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        int rows = Math.min(sorted.size(), Math.max(1, KrylixConfig.get().mobStatsMaxEntries));
        for (int i = 0; i < rows; i++) {
            Map.Entry<String, Integer> entry = sorted.get(i);
            int y = Y + i * ROW;
            Avatars.draw(graphics, null, entry.getKey(), X, y + 2, ICON);
            graphics.text(font, String.valueOf(entry.getValue()), X + ICON + PAD, y + 4, 0xFFFFFFFF);
        }
    }
}
