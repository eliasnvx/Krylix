package com.eliasnvx.krylix.forge.client

import com.eliasnvx.krylix.forge.network.NetworkPackets.PlayerStatsSyncPacket.PlayerStatEntry
import com.mojang.authlib.GameProfile
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import java.util.UUID
import kotlin.math.max

/**
 * GUI-экран рейтинга — тёмная полупрозрачная скруглённая панель со скроллящимся списком, в стиле
 * магазинных/каталожных GUI, а не голый список в чате. Две вкладки, обе про игроков: PvP-рейтинг
 * (килы/смерти игроков друг друга) и рейтинг по личным убийствам мобов (кто из игроков убил
 * больше мобов — не список видов мобов).
 */
class LeaderboardScreen : Screen(Component.translatable("screen.krylix.leaderboard")) {

    private enum class Mode { PLAYERS, MOB_KILLS }

    private val panelWidth = 340
    private val panelHeight = 260
    private val rowHeight = 22
    private val headerHeight = 58
    private val footerHeight = 18
    private val avatarSize = 16
    private val tabHeight = 16

    /** Правая граница колонки ранга ("#150" и т.п. правым краем упирается сюда, не наезжая на аватар) */
    private val rankColumnEnd = 40
    private val avatarColumnX = 46

    private val pageSize = 60

    private var mode = Mode.PLAYERS
    private var page = 0
    private var scrollOffset = 0
    private var prevButtonRect: IntArray? = null
    private var nextButtonRect: IntArray? = null

    private val playerEntries: List<PlayerStatEntry> = PlayerStatsClient.sortedByKills()
    private val mobKillEntries: List<PlayerStatEntry> = PlayerStatsClient.sortedByMobKills()
    private val headTextures = mutableMapOf<String, ResourceLocation?>()

    override fun init() {
        (playerEntries + mobKillEntries).forEach { entry -> headTextures.getOrPut(entry.uuid) { resolveSkin(entry) } }
    }

    private fun resolveSkin(entry: PlayerStatEntry): ResourceLocation? {
        val uuid = runCatching { UUID.fromString(entry.uuid) }.getOrNull() ?: return null
        val connection = minecraft?.connection
        connection?.getPlayerInfo(uuid)?.skinLocation?.let { return it }
        return runCatching { minecraft?.skinManager?.getInsecureSkinLocation(GameProfile(uuid, entry.name)) }.getOrNull()
    }

    private fun panelX() = (width - panelWidth) / 2
    private fun panelY() = (height - panelHeight) / 2
    private fun listTop() = panelY() + headerHeight
    private fun listBottom() = panelY() + panelHeight - footerHeight
    private fun listHeight() = listBottom() - listTop()

    private fun fullEntries() = if (mode == Mode.PLAYERS) playerEntries else mobKillEntries
    private fun totalPages() = max(1, (fullEntries().size + pageSize - 1) / pageSize)
    private fun pageEntries(): List<PlayerStatEntry> {
        val full = fullEntries()
        val from = (page * pageSize).coerceIn(0, full.size)
        val to = ((page + 1) * pageSize).coerceIn(0, full.size)
        return full.subList(from, to)
    }

    /** Строк в скролл-зоне: сами записи + виртуальная строка с кнопками страниц (если страниц > 1) */
    private fun rowCount() = pageEntries().size + if (totalPages() > 1) 1 else 0
    private fun maxScroll() = max(0, rowCount() * rowHeight - listHeight())

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(guiGraphics)

        val px = panelX()
        val py = panelY()

        HudRender.rounded(guiGraphics, px, py, panelWidth, panelHeight, 6) {
            guiGraphics.fill(px, py, px + panelWidth, py + panelHeight, 0xE6101014.toInt())
        }

        guiGraphics.drawCenteredString(font, title, px + panelWidth / 2, py + 8, 0xFFFFFF)

        val closeX = px + panelWidth - 18
        val closeY = py + 7
        val closeHovered = mouseX in closeX..(closeX + 12) && mouseY in closeY..(closeY + 12)
        guiGraphics.drawString(font, "x", closeX + 3, closeY + 2, if (closeHovered) 0xFFFFFF else 0x999999)

        renderTabs(guiGraphics, mouseX, mouseY, px, py)

        // Заголовки колонок
        val headerY = py + headerHeight - 20
        val statsHeaderText = if (mode == Mode.PLAYERS) {
            Component.translatable("krylix.leaderboard.header_kd").string
        } else {
            Component.translatable("krylix.leaderboard.tab_mob_kills").string
        }
        guiGraphics.drawString(font, Component.translatable("krylix.leaderboard.header_rank").string, px + 12, headerY, 0x888888)
        guiGraphics.drawString(font, Component.translatable("krylix.leaderboard.header_player").string, px + avatarColumnX, headerY, 0x888888)
        val statsHeaderWidth = font.width(statsHeaderText)
        guiGraphics.drawString(font, statsHeaderText, px + panelWidth - statsHeaderWidth - 12, headerY, 0x888888)
        guiGraphics.fill(px + 8, py + headerHeight - 4, px + panelWidth - 8, py + headerHeight - 3, 0x40FFFFFF)

        if (fullEntries().isEmpty()) {
            val emptyKey = if (mode == Mode.PLAYERS) "screen.krylix.leaderboard.empty_players" else "screen.krylix.leaderboard.empty_mob_kills"
            val emptyText = Component.translatable(emptyKey).string
            guiGraphics.drawCenteredString(font, emptyText, px + panelWidth / 2, listTop() + listHeight() / 2 - 4, 0xAAAAAA)
        } else {
            renderRows(guiGraphics, mouseX, mouseY)
        }

        val footerText = Component.translatable("krylix.leaderboard.footer", fullEntries().size).string
        guiGraphics.drawCenteredString(font, footerText, px + panelWidth / 2, py + panelHeight - 14, 0x777777)

        super.render(guiGraphics, mouseX, mouseY, partialTick)
    }

    private fun tabRect(index: Int, px: Int, py: Int): IntArray {
        val tabWidth = 90
        val gap = 6
        val totalWidth = tabWidth * 2 + gap
        val startX = px + (panelWidth - totalWidth) / 2
        val x = startX + index * (tabWidth + gap)
        val y = py + 20
        return intArrayOf(x, y, tabWidth, tabHeight)
    }

    private fun renderTabs(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, px: Int, py: Int) {
        renderTab(guiGraphics, mouseX, mouseY, tabRect(0, px, py), Component.translatable("krylix.leaderboard.tab_pvp").string, Mode.PLAYERS)
        renderTab(guiGraphics, mouseX, mouseY, tabRect(1, px, py), Component.translatable("krylix.leaderboard.tab_mob_kills").string, Mode.MOB_KILLS)
    }

    private fun renderTab(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, rect: IntArray, label: String, tabMode: Mode) {
        val (x, y, w, h) = rect
        val active = mode == tabMode
        val hovered = mouseX in x..(x + w) && mouseY in y..(y + h)
        HudRender.rounded(guiGraphics, x, y, w, h, 3) {
            val bg = when {
                active -> 0xCC3A7BD5.toInt()
                hovered -> 0x30FFFFFF
                else -> 0x20FFFFFF
            }
            guiGraphics.fill(x, y, x + w, y + h, bg)
        }
        val textColor = if (active) 0xFFFFFF else 0xAAAAAA
        val textWidth = font.width(label)
        guiGraphics.drawString(font, label, x + (w - textWidth) / 2, y + (h - 8) / 2, textColor)
    }

    private operator fun IntArray.component1() = this[0]
    private operator fun IntArray.component2() = this[1]
    private operator fun IntArray.component3() = this[2]
    private operator fun IntArray.component4() = this[3]

    private fun renderRows(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val px = panelX()
        val top = listTop()
        val bottom = listBottom()
        prevButtonRect = null
        nextButtonRect = null

        HudRender.rounded(guiGraphics, px + 6, top, panelWidth - 12, bottom - top, 4) {
            val localUuid = minecraft?.player?.uuid?.toString()
            val entries = pageEntries()

            var y = top - scrollOffset
            for (index in entries.indices) {
                if (y + rowHeight >= top && y <= bottom) {
                    val entry = entries[index]
                    val rowHovered = mouseX in (px + 6)..(px + panelWidth - 6) && mouseY in y..(y + rowHeight)
                    val isSelf = entry.uuid == localUuid
                    val bgColor = when {
                        rowHovered -> 0x33FFFFFF
                        isSelf -> 0x2255AAFF.toInt()
                        index % 2 == 0 -> 0x18FFFFFF
                        else -> 0x00000000
                    }
                    if (bgColor != 0) {
                        guiGraphics.fill(px + 6, y, px + panelWidth - 6, y + rowHeight, bgColor)
                    }

                    renderRow(guiGraphics, entry, page * pageSize + index, px, y)
                }
                y += rowHeight
            }

            if (totalPages() > 1 && y + rowHeight >= top && y <= bottom) {
                renderPaginationRow(guiGraphics, mouseX, mouseY, px, y)
            }
        }
    }

    private fun renderPaginationRow(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, px: Int, y: Int) {
        val btnSize = 16
        val gap = 8
        val pageText = Component.translatable("krylix.leaderboard.page", page + 1, totalPages()).string
        val pageTextWidth = font.width(pageText)
        val totalWidth = btnSize + gap + pageTextWidth + gap + btnSize
        val startX = px + (panelWidth - totalWidth) / 2
        val btnY = y + (rowHeight - btnSize) / 2

        val hasPrev = page > 0
        val hasNext = page < totalPages() - 1

        val prevRect = intArrayOf(startX, btnY, btnSize, btnSize)
        val nextRect = intArrayOf(startX + btnSize + gap + pageTextWidth + gap, btnY, btnSize, btnSize)
        prevButtonRect = prevRect
        nextButtonRect = nextRect

        renderPageButton(guiGraphics, mouseX, mouseY, prevRect, pointRight = false, hasPrev)
        renderPageButton(guiGraphics, mouseX, mouseY, nextRect, pointRight = true, hasNext)
        guiGraphics.drawString(font, pageText, startX + btnSize + gap, y + (rowHeight - 8) / 2, 0xCCCCCC)
    }

    private fun renderPageButton(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, rect: IntArray, pointRight: Boolean, enabled: Boolean) {
        val (x, y, w, h) = rect
        val hovered = enabled && mouseX in x..(x + w) && mouseY in y..(y + h)
        HudRender.rounded(guiGraphics, x, y, w, h, 3) {
            val bg = when {
                !enabled -> 0x15FFFFFF
                hovered -> 0x40FFFFFF
                else -> 0x25FFFFFF
            }
            guiGraphics.fill(x, y, x + w, y + h, bg)
        }
        val color = if (enabled) 0xFFFFFFFF.toInt() else 0xFF555555.toInt()
        renderArrow(guiGraphics, x, y, w, h, pointRight, color)
    }

    /**
     * Рисует стрелку пикселями вместо символов "<"/">" — глиф ">" во встроенном шрифте Minecraft
     * не является зеркальным отражением "<" по метрикам, из-за чего центрирование текстом выглядело
     * визуально смещённым. Треугольник строится геометрически, поэтому обе стрелки гарантированно
     * симметричны друг другу.
     */
    private fun renderArrow(guiGraphics: GuiGraphics, x: Int, y: Int, w: Int, h: Int, pointRight: Boolean, color: Int) {
        val arrowW = 4
        val arrowH = 7
        val originX = x + (w - arrowW) / 2
        val originY = y + (h - arrowH) / 2
        val mid = arrowH / 2
        for (row in 0 until arrowH) {
            val d = kotlin.math.abs(row - mid)
            val length = (arrowW - d).coerceAtLeast(1)
            val rowY = originY + row
            if (pointRight) {
                guiGraphics.fill(originX, rowY, originX + length, rowY + 1, color)
            } else {
                guiGraphics.fill(originX + (arrowW - length), rowY, originX + arrowW, rowY + 1, color)
            }
        }
    }

    private fun renderRow(guiGraphics: GuiGraphics, entry: PlayerStatEntry, rank: Int, px: Int, y: Int) {
        val textY = y + (rowHeight - 8) / 2
        val avatarY = y + (rowHeight - avatarSize) / 2

        val rankText = "#${rank + 1}"
        val rankX = px + rankColumnEnd - font.width(rankText)
        guiGraphics.drawString(font, rankText, rankX, textY, 0xAAAAAA)

        val avatarX = px + avatarColumnX
        val texture = headTextures[entry.uuid]
        HudRender.rounded(guiGraphics, avatarX, avatarY, avatarSize, 2) {
            if (texture != null) {
                RenderSystem.setShaderTexture(0, texture)
                RenderSystem.enableBlend()
                guiGraphics.blit(texture, avatarX, avatarY, avatarSize, avatarSize, 8.0f, 8.0f, 8, 8, 64, 64)
                guiGraphics.blit(texture, avatarX, avatarY, avatarSize, avatarSize, 40.0f, 8.0f, 8, 8, 64, 64)
                RenderSystem.disableBlend()
            } else {
                guiGraphics.fill(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, 0xFF555555.toInt())
            }
        }

        guiGraphics.drawString(font, entry.name, avatarX + avatarSize + 6, textY, 0xFFFFFF)

        val statsText = if (mode == Mode.PLAYERS) {
            val kd = if (entry.deaths == 0) entry.kills.toDouble() else entry.kills.toDouble() / entry.deaths
            "${entry.kills}K / ${entry.deaths}D  (${"%.2f".format(kd)})"
        } else {
            Component.translatable("krylix.leaderboard.mob_kills_count", entry.mobKills).string
        }
        val statsWidth = font.width(statsText)
        guiGraphics.drawString(font, statsText, px + panelWidth - statsWidth - 12, textY, 0xCCCCCC)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, delta: Double): Boolean {
        scrollOffset = (scrollOffset - (delta * rowHeight * 1.5).toInt()).coerceIn(0, maxScroll())
        return true
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val px = panelX()
        val py = panelY()
        val closeX = px + panelWidth - 18
        val closeY = py + 7
        if (mouseX >= closeX && mouseX <= closeX + 12 && mouseY >= closeY && mouseY <= closeY + 12) {
            onClose()
            return true
        }

        val (t0x, t0y, t0w, t0h) = tabRect(0, px, py)
        if (mouseX >= t0x && mouseX <= t0x + t0w && mouseY >= t0y && mouseY <= t0y + t0h) {
            mode = Mode.PLAYERS
            page = 0
            scrollOffset = 0
            return true
        }
        val (t1x, t1y, t1w, t1h) = tabRect(1, px, py)
        if (mouseX >= t1x && mouseX <= t1x + t1w && mouseY >= t1y && mouseY <= t1y + t1h) {
            mode = Mode.MOB_KILLS
            page = 0
            scrollOffset = 0
            return true
        }

        prevButtonRect?.let { (bx, by, bw, bh) ->
            if (page > 0 && mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh) {
                page--
                scrollOffset = 0
                return true
            }
        }
        nextButtonRect?.let { (bx, by, bw, bh) ->
            if (page < totalPages() - 1 && mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh) {
                page++
                scrollOffset = 0
                return true
            }
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun isPauseScreen(): Boolean = false
}
