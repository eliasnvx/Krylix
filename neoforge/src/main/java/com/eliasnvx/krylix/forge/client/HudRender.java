package com.eliasnvx.krylix.forge.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public class HudRender {
    public static void roundedFill(GuiGraphicsExtractor guiGraphics, int x, int y, int size, int cornerCut, int color) {
        roundedFill(guiGraphics, x, y, size, size, cornerCut, color);
    }

    public static void roundedFill(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, int cornerCut, int color) {
        if (cornerCut <= 0 || cornerCut * 2 >= width || cornerCut * 2 >= height) {
            guiGraphics.fill(x, y, x + width, y + height, color);
            return;
        }

        // Top horizontal strip
        guiGraphics.fill(x + cornerCut, y, x + width - cornerCut, y + cornerCut, color);
        // Middle full-width body
        guiGraphics.fill(x, y + cornerCut, x + width, y + height - cornerCut, color);
        // Bottom horizontal strip
        guiGraphics.fill(x + cornerCut, y + height - cornerCut, x + width - cornerCut, y + height, color);
    }
}
