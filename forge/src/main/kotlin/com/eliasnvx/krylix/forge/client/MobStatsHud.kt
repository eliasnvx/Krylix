package com.eliasnvx.krylix.forge.client

import com.eliasnvx.krylix.forge.config.KrylixConfig
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics

/**
 * HUD-панель в левом верхнем углу с количеством убийств враждебных мобов за весь мир.
 * Данные приходят с сервера через MobStatsSyncPacket и кэшируются в MobStatsClient
 * (per-world SavedData, см. MobKillStatsData).
 */
object MobStatsHud {
    private const val iconSize = 12
    private const val rowHeight = 14
    private const val padding = 4
    private const val iconCornerCut = 2 // Срез уголков иконки в px (лёгкое скругление)

    /** Тумблер — сохраняется в конфиг, не сбрасывается при перезапуске */
    fun setEnabled(enabled: Boolean) {
        val config = KrylixConfig.get()
        config.mobStatsEnabled = enabled
        KrylixConfig.save()
    }

    fun isStatsEnabled(): Boolean = KrylixConfig.get().mobStatsEnabled

    fun render(guiGraphics: GuiGraphics, @Suppress("UNUSED_PARAMETER") partialTick: Float) {
        val config = KrylixConfig.get()
        val topEntries = MobStatsClient.sortedByCount().take(config.mobStatsMaxEntries)
        if (!config.mobStatsEnabled || topEntries.isEmpty()) return

        val minecraft = Minecraft.getInstance()
        val font = minecraft.font

        val x = 5
        var y = 5
        for ((entityId, count) in topEntries) {
            val texture = MobTextures.byEntityId(entityId)
            HudRender.rounded(guiGraphics, x, y, iconSize, iconCornerCut) {
                if (texture != null) {
                    MobTextures.blitMobFace(guiGraphics, texture, entityId, x, y, iconSize)
                } else {
                    guiGraphics.fill(x, y, x + iconSize, y + iconSize, 0x80808080.toInt())
                }
            }
            guiGraphics.drawString(font, "${MobStatsClient.displayName(entityId)}: $count", x + iconSize + padding, y + 2, 0xFFFFFF)
            y += rowHeight
        }
    }
}
