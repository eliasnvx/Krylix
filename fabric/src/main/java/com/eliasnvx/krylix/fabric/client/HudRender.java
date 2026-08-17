package com.eliasnvx.krylix.fabric.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public class HudRender {
    public static void rounded(GuiGraphicsExtractor guiGraphics, int x, int y, int size, int cornerCut, Runnable paint) {
        rounded(guiGraphics, x, y, size, size, cornerCut, paint);
    }

    public static void rounded(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, int cornerCut, Runnable paint) {
        if (cornerCut <= 0 || cornerCut * 2 >= width || cornerCut * 2 >= height) {
            paint.run();
            return;
        }

        guiGraphics.enableScissor(x + cornerCut, y, x + width - cornerCut, y + cornerCut);
        paint.run();
        guiGraphics.disableScissor();

        guiGraphics.enableScissor(x, y + cornerCut, x + width, y + height - cornerCut);
        paint.run();
        guiGraphics.disableScissor();

        guiGraphics.enableScissor(x + cornerCut, y + height - cornerCut, x + width - cornerCut, y + height);
        paint.run();
        guiGraphics.disableScissor();
    }
}
