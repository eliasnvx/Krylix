package com.eliasnvx.krylix.fabric.client;

import net.minecraft.client.renderer.RenderPipelines;
import com.eliasnvx.krylix.fabric.network.FabricNetworkPackets.PlayerStatEntry;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LeaderboardScreen extends Screen {
    private enum Mode { PLAYERS, MOB_KILLS }

    private final int panelWidth = 340;
    private final int panelHeight = 260;
    private final int rowHeight = 22;
    private final int headerHeight = 58;
    private final int footerHeight = 18;
    private final int avatarSize = 16;
    private final int tabHeight = 16;

    private final int rankColumnEnd = 40;
    private final int avatarColumnX = 46;

    private final int pageSize = 60;

    private Mode mode = Mode.PLAYERS;
    private int page = 0;
    private int scrollOffset = 0;
    private int[] prevButtonRect = null;
    private int[] nextButtonRect = null;

    private final List<PlayerStatEntry> playerEntries = PlayerStatsClient.sortedByKills();
    private final List<PlayerStatEntry> mobKillEntries = PlayerStatsClient.sortedByMobKills();
    private final Map<String, Identifier> headTextures = new HashMap<>();

    public LeaderboardScreen() {
        super(Component.translatable("screen.krylix.leaderboard"));
    }

    @Override
    protected void init() {
        for (PlayerStatEntry entry : playerEntries) {
            headTextures.computeIfAbsent(entry.uuid(), k -> resolveSkin(entry));
        }
        for (PlayerStatEntry entry : mobKillEntries) {
            headTextures.computeIfAbsent(entry.uuid(), k -> resolveSkin(entry));
        }
    }

    private Identifier resolveSkin(PlayerStatEntry entry) {
        try {
            UUID uuid = UUID.fromString(entry.uuid());
            if (minecraft != null && minecraft.getConnection() != null) {
                PlayerInfo info = minecraft.getConnection().getPlayerInfo(uuid);
                if (info != null && info.getSkin() != null) {
                    return info.getSkin().body().texturePath();
                }
                return net.minecraft.client.resources.DefaultPlayerSkin.get(uuid).body().texturePath();
            }
        } catch (Exception ignored) { }
        return null;
    }

    private int panelX() { return (width - panelWidth) / 2; }
    private int panelY() { return (height - panelHeight) / 2; }
    private int listTop() { return panelY() + headerHeight; }
    private int listBottom() { return panelY() + panelHeight - footerHeight; }
    private int listHeight() { return listBottom() - listTop(); }

    private List<PlayerStatEntry> fullEntries() {
        return mode == Mode.PLAYERS ? playerEntries : mobKillEntries;
    }

    private int totalPages() {
        return Math.max(1, (fullEntries().size() + pageSize - 1) / pageSize);
    }

    private List<PlayerStatEntry> pageEntries() {
        List<PlayerStatEntry> full = fullEntries();
        int from = Math.max(0, Math.min(full.size(), page * pageSize));
        int to = Math.max(0, Math.min(full.size(), (page + 1) * pageSize));
        return full.subList(from, to);
    }

    private int rowCount() {
        return pageEntries().size() + (totalPages() > 1 ? 1 : 0);
    }

    private int maxScroll() {
        return Math.max(0, rowCount() * rowHeight - listHeight());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        int px = panelX();
        int py = panelY();

        HudRender.rounded(guiGraphics, px, py, panelWidth, panelHeight, 6, () -> {
            guiGraphics.fill(px, py, px + panelWidth, py + panelHeight, 0xE6101014);
        });

        guiGraphics.centeredText(font, title, px + panelWidth / 2, py + 8, 0xFFFFFF);

        int closeX = px + panelWidth - 18;
        int closeY = py + 7;
        boolean closeHovered = mouseX >= closeX && mouseX <= closeX + 12 && mouseY >= closeY && mouseY <= closeY + 12;
        guiGraphics.text(font, "x", closeX + 3, closeY + 2, closeHovered ? 0xFFFFFF : 0x999999);

        renderTabs(guiGraphics, mouseX, mouseY, px, py);

        int headerY = py + headerHeight - 20;
        String statsHeaderText = mode == Mode.PLAYERS ?
                Component.translatable("krylix.leaderboard.header_kd").getString() :
                Component.translatable("krylix.leaderboard.tab_mob_kills").getString();
        String rankHeaderText = Component.translatable("krylix.leaderboard.header_rank").getString();
        
        guiGraphics.text(font, rankHeaderText, px + 12, headerY, 0x888888);
        guiGraphics.text(font, Component.translatable("krylix.leaderboard.header_player").getString(), px + avatarColumnX, headerY, 0x888888);
        int statsHeaderWidth = font.width(statsHeaderText);
        guiGraphics.text(font, statsHeaderText, px + panelWidth - statsHeaderWidth - 12, headerY, 0x888888);
        guiGraphics.fill(px + 8, py + headerHeight - 4, px + panelWidth - 8, py + headerHeight - 3, 0x40FFFFFF);

        if (fullEntries().isEmpty()) {
            String emptyKey = mode == Mode.PLAYERS ? "screen.krylix.leaderboard.empty_players" : "screen.krylix.leaderboard.empty_mob_kills";
            String emptyText = Component.translatable(emptyKey).getString();
            guiGraphics.centeredText(font, emptyText, px + panelWidth / 2, listTop() + listHeight() / 2 - 4, 0xAAAAAA);
        } else {
            renderRows(guiGraphics, mouseX, mouseY);
        }

        String footerText = Component.translatable("krylix.leaderboard.footer", fullEntries().size()).getString();
        guiGraphics.centeredText(font, footerText, px + panelWidth / 2, py + panelHeight - 14, 0x777777);
    }

    private int[] tabRect(int index, int px, int py) {
        int tabWidth = 90;
        int gap = 6;
        int totalWidth = tabWidth * 2 + gap;
        int startX = px + (panelWidth - totalWidth) / 2;
        int x = startX + index * (tabWidth + gap);
        int y = py + 20;
        return new int[]{x, y, tabWidth, tabHeight};
    }

    private void renderTabs(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, int px, int py) {
        renderTab(guiGraphics, mouseX, mouseY, tabRect(0, px, py), Component.translatable("krylix.leaderboard.tab_pvp").getString(), Mode.PLAYERS);
        renderTab(guiGraphics, mouseX, mouseY, tabRect(1, px, py), Component.translatable("krylix.leaderboard.tab_mob_kills").getString(), Mode.MOB_KILLS);
    }

    private void renderTab(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, int[] rect, String label, Mode tabMode) {
        int x = rect[0], y = rect[1], w = rect[2], h = rect[3];
        boolean active = mode == tabMode;
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        
        HudRender.rounded(guiGraphics, x, y, w, h, 3, () -> {
            int bg = active ? 0xCC3A7BD5 : (hovered ? 0x30FFFFFF : 0x20FFFFFF);
            guiGraphics.fill(x, y, x + w, y + h, bg);
        });
        
        int textColor = active ? 0xFFFFFF : 0xAAAAAA;
        int textWidth = font.width(label);
        guiGraphics.text(font, label, x + (w - textWidth) / 2, y + (h - 8) / 2, textColor);
    }

    private void renderRows(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int px = panelX();
        int top = listTop();
        int bottom = listBottom();
        prevButtonRect = null;
        nextButtonRect = null;

        HudRender.rounded(guiGraphics, px + 6, top, panelWidth - 12, bottom - top, 4, () -> {
            String localUuid = minecraft != null && minecraft.player != null ? minecraft.player.getUUID().toString() : null;
            List<PlayerStatEntry> entries = pageEntries();

            int y = top - scrollOffset;
            for (int i = 0; i < entries.size(); i++) {
                if (y + rowHeight >= top && y <= bottom) {
                    PlayerStatEntry entry = entries.get(i);
                    boolean rowHovered = mouseX >= px + 6 && mouseX <= px + panelWidth - 6 && mouseY >= y && mouseY <= y + rowHeight;
                    boolean isSelf = entry.uuid().equals(localUuid);
                    
                    int bgColor = 0;
                    if (rowHovered) bgColor = 0x33FFFFFF;
                    else if (isSelf) bgColor = 0x2255AAFF;
                    else if (i % 2 == 0) bgColor = 0x18FFFFFF;
                    
                    if (bgColor != 0) {
                        guiGraphics.fill(px + 6, y, px + panelWidth - 6, y + rowHeight, bgColor);
                    }

                    renderRow(guiGraphics, entry, page * pageSize + i, px, y);
                }
                y += rowHeight;
            }

            if (totalPages() > 1 && y + rowHeight >= top && y <= bottom) {
                renderPaginationRow(guiGraphics, mouseX, mouseY, px, y);
            }
        });
    }

    private void renderPaginationRow(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, int px, int y) {
        int btnSize = 16;
        int gap = 8;
        String pageText = Component.translatable("krylix.leaderboard.page", page + 1, totalPages()).getString();
        int pageTextWidth = font.width(pageText);
        int totalWidth = btnSize + gap + pageTextWidth + gap + btnSize;
        int startX = px + (panelWidth - totalWidth) / 2;
        int btnY = y + (rowHeight - btnSize) / 2;

        boolean hasPrev = page > 0;
        boolean hasNext = page < totalPages() - 1;

        prevButtonRect = new int[]{startX, btnY, btnSize, btnSize};
        nextButtonRect = new int[]{startX + btnSize + gap + pageTextWidth + gap, btnY, btnSize, btnSize};

        renderPageButton(guiGraphics, mouseX, mouseY, prevButtonRect, false, hasPrev);
        renderPageButton(guiGraphics, mouseX, mouseY, nextButtonRect, true, hasNext);
        guiGraphics.text(font, pageText, startX + btnSize + gap, y + (rowHeight - 8) / 2, 0xCCCCCC);
    }

    private void renderPageButton(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, int[] rect, boolean pointRight, boolean enabled) {
        int x = rect[0], y = rect[1], w = rect[2], h = rect[3];
        boolean hovered = enabled && mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        
        HudRender.rounded(guiGraphics, x, y, w, h, 3, () -> {
            int bg = !enabled ? 0x15FFFFFF : (hovered ? 0x40FFFFFF : 0x25FFFFFF);
            guiGraphics.fill(x, y, x + w, y + h, bg);
        });
        
        int color = enabled ? 0xFFFFFFFF : 0xFF555555;
        renderArrow(guiGraphics, x, y, w, h, pointRight, color);
    }

    private void renderArrow(GuiGraphicsExtractor guiGraphics, int x, int y, int w, int h, boolean pointRight, int color) {
        int arrowW = 4;
        int arrowH = 7;
        int originX = x + (w - arrowW) / 2;
        int originY = y + (h - arrowH) / 2;
        int mid = arrowH / 2;
        for (int row = 0; row < arrowH; row++) {
            int d = Math.abs(row - mid);
            int length = Math.max(1, arrowW - d);
            int rowY = originY + row;
            if (pointRight) {
                guiGraphics.fill(originX, rowY, originX + length, rowY + 1, color);
            } else {
                guiGraphics.fill(originX + (arrowW - length), rowY, originX + arrowW, rowY + 1, color);
            }
        }
    }

    private void renderRow(GuiGraphicsExtractor guiGraphics, PlayerStatEntry entry, int rank, int px, int y) {
        int textY = y + (rowHeight - 8) / 2;
        int avatarY = y + (rowHeight - avatarSize) / 2;

        String rankText = "#" + (rank + 1);
        int rankX = px + rankColumnEnd - font.width(rankText);
        guiGraphics.text(font, rankText, rankX, textY, 0xAAAAAA);

        int avatarX = px + avatarColumnX;
        Identifier texture = headTextures.get(entry.uuid());
        
        HudRender.rounded(guiGraphics, avatarX, avatarY, avatarSize, 2, () -> {
            if (texture != null) {
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, avatarX, avatarY, 8.0f, 8.0f, 8, 8, 64, 64, avatarSize, avatarSize);
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, avatarX, avatarY, 40.0f, 8.0f, 8, 8, 64, 64, avatarSize, avatarSize);
            } else {
                guiGraphics.fill(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, 0xFF555555);
            }
        });

        guiGraphics.text(font, entry.name(), avatarX + avatarSize + 6, textY, 0xFFFFFF);

        String statsText;
        if (mode == Mode.PLAYERS) {
            double kd = entry.deaths() == 0 ? entry.kills() : (double) entry.kills() / entry.deaths();
            statsText = entry.kills() + "K / " + entry.deaths() + "D  (" + String.format("%.2f", kd) + ")";
        } else {
            statsText = Component.translatable("krylix.leaderboard.mob_kills_count", entry.mobKills()).getString();
        }
        int statsWidth = font.width(statsText);
        guiGraphics.text(font, statsText, px + panelWidth - statsWidth - 12, textY, 0xCCCCCC);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset = Math.max(0, Math.min(maxScroll(), scrollOffset - (int) (scrollY * rowHeight * 1.5)));
        return true;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int px = panelX();
        int py = panelY();
        int closeX = px + panelWidth - 18;
        int closeY = py + 7;
        
        if (mouseX >= closeX && mouseX <= closeX + 12 && mouseY >= closeY && mouseY <= closeY + 12) {
            this.onClose();
            return true;
        }

        int[] t0 = tabRect(0, px, py);
        if (mouseX >= t0[0] && mouseX <= t0[0] + t0[2] && mouseY >= t0[1] && mouseY <= t0[1] + t0[3]) {
            mode = Mode.PLAYERS;
            page = 0;
            scrollOffset = 0;
            return true;
        }
        
        int[] t1 = tabRect(1, px, py);
        if (mouseX >= t1[0] && mouseX <= t1[0] + t1[2] && mouseY >= t1[1] && mouseY <= t1[1] + t1[3]) {
            mode = Mode.MOB_KILLS;
            page = 0;
            scrollOffset = 0;
            return true;
        }

        if (prevButtonRect != null) {
            int[] r = prevButtonRect;
            if (page > 0 && mouseX >= r[0] && mouseX <= r[0] + r[2] && mouseY >= r[1] && mouseY <= r[1] + r[3]) {
                page--;
                scrollOffset = 0;
                return true;
            }
        }
        if (nextButtonRect != null) {
            int[] r = nextButtonRect;
            if (page < totalPages() - 1 && mouseX >= r[0] && mouseX <= r[0] + r[2] && mouseY >= r[1] && mouseY <= r[1] + r[3]) {
                page++;
                scrollOffset = 0;
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
