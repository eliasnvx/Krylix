package com.eliasnvx.krylix.forge.client

import net.minecraft.client.gui.GuiGraphics

/**
 * Общие low-level рендер-хелперы HUD, не привязанные к конкретным текстурам мобов
 * (в отличие от [MobTextures]).
 */
object HudRender {
    /** Квадратный вариант (аватарки) — см. [rounded] с раздельными width/height. */
    inline fun rounded(guiGraphics: GuiGraphics, x: Int, y: Int, size: Int, cornerCut: Int, paint: () -> Unit) {
        rounded(guiGraphics, x, y, size, size, cornerCut, paint)
    }

    /**
     * Рисует содержимое [paint] внутри прямоугольника (x, y, width, height), обрезая уголки на
     * [cornerCut] px — лёгкое "скругление" без стенсил-шейдеров. Scissor умеет только прямоугольники,
     * поэтому режем тремя горизонтальными полосами (узкая сверху, полная посередине, узкая снизу).
     */
    inline fun rounded(guiGraphics: GuiGraphics, x: Int, y: Int, width: Int, height: Int, cornerCut: Int, paint: () -> Unit) {
        if (cornerCut <= 0 || cornerCut * 2 >= width || cornerCut * 2 >= height) {
            paint()
            return
        }

        guiGraphics.enableScissor(x + cornerCut, y, x + width - cornerCut, y + cornerCut)
        paint()
        guiGraphics.disableScissor()

        guiGraphics.enableScissor(x, y + cornerCut, x + width, y + height - cornerCut)
        paint()
        guiGraphics.disableScissor()

        guiGraphics.enableScissor(x + cornerCut, y + height - cornerCut, x + width - cornerCut, y + height)
        paint()
        guiGraphics.disableScissor()
    }
}
