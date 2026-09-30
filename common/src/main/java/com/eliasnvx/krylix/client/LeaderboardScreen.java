package com.eliasnvx.krylix.client;

import com.eliasnvx.krylix.network.KrylixPayloads.LeaderboardRow;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** The all-time leaderboard: PvP and mob-kill tabs, fetched from the server when it opens. */
public class LeaderboardScreen extends Screen {
    private enum Mode { PLAYERS, MOB_KILLS }

    private static final int MAX_PANEL_WIDTH = 340;
    private static final int MAX_PANEL_HEIGHT = 260;
    private static final int SCREEN_MARGIN = 8;
    private static final int ROW_HEIGHT = 22;
    private static final int HEADER_HEIGHT = 58;
    private static final int FOOTER_HEIGHT = 18;
    private static final int AVATAR_SIZE = 16;
    private static final int TAB_WIDTH = 90;
    private static final int TAB_HEIGHT = 16;
    private static final int RANK_COLUMN_END = 40;
    private static final int AVATAR_COLUMN_X = 46;
    private static final int PAGE_SIZE = 60;

    /** Right edges of the stat columns, measured from the panel's right edge. */
    private static final int COL_KD = 12;
    private static final int COL_DEATHS = 58;
    private static final int COL_KILLS = 104;

    private static final int HEADER_COLOR = 0xFF888888;

    private Mode mode = Mode.PLAYERS;
    private int page;
    private int scrollOffset;
    private int[] prevButtonRect;
    private int[] nextButtonRect;
    private boolean requested;

    // Labels, resolved once per init (the language can only change with this screen closed)
    private String rankLabel = "";
    private String playerLabel = "";
    private String killsLabel = "";
    private String deathsLabel = "";
    private String kdLabel = "";
    private String pvpTab = "";
    private String mobTab = "";

    /** The visible page as ready-to-draw strings, rebuilt only when the data, tab, page or width change. */
    private record RowText(LeaderboardRow row, String rank, String name, String first, String second, String third) {
    }

    private List<RowText> rows = List.of();
    private String rowsKey = "";

    public LeaderboardScreen() {
        super(Component.translatable("screen.krylix.leaderboard"));
    }

    @Override
    protected void init() {
        // init() runs again on every resize: ask the server only once
        if (!requested) {
            requested = true;
            LeaderboardClient.request();
        }
        rankLabel = Component.translatable("krylix.leaderboard.header_rank").getString();
        playerLabel = Component.translatable("krylix.leaderboard.header_player").getString();
        killsLabel = Component.translatable("krylix.leaderboard.header_kills").getString();
        deathsLabel = Component.translatable("krylix.leaderboard.header_deaths").getString();
        kdLabel = Component.translatable("krylix.leaderboard.header_kd_ratio").getString();
        pvpTab = Component.translatable("krylix.leaderboard.tab_pvp").getString();
        mobTab = Component.translatable("krylix.leaderboard.tab_mob_kills").getString();
        rowsKey = "";
        clampScroll();
    }

    @Override
    public void tick() {
        LeaderboardClient.retryIfStuck();
    }

    /** Opens the Mob Kills tab (also used by the screenshot test). */
    public void showMobKills() {
        switchTo(Mode.MOB_KILLS);
    }

    private void switchTo(Mode newMode) {
        mode = newMode;
        page = 0;
        scrollOffset = 0;
    }

    private int panelWidth() { return Math.min(MAX_PANEL_WIDTH, width - 2 * SCREEN_MARGIN); }
    private int panelHeight() { return Math.min(MAX_PANEL_HEIGHT, height - 2 * SCREEN_MARGIN); }
    private int panelX() { return (width - panelWidth()) / 2; }
    private int panelY() { return (height - panelHeight()) / 2; }
    private int listTop() { return panelY() + HEADER_HEIGHT; }
    private int listBottom() { return panelY() + panelHeight() - FOOTER_HEIGHT; }
    private int listHeight() { return Math.max(0, listBottom() - listTop()); }

    private List<LeaderboardRow> fullEntries() {
        return mode == Mode.PLAYERS ? LeaderboardClient.byKills() : LeaderboardClient.byMobKills();
    }

    private int totalPages() {
        return Math.max(1, (fullEntries().size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private List<LeaderboardRow> pageEntries() {
        List<LeaderboardRow> full = fullEntries();
        int from = Math.clamp((long) page * PAGE_SIZE, 0, full.size());
        int to = Math.clamp((long) (page + 1) * PAGE_SIZE, 0, full.size());
        return full.subList(from, to);
    }

    private int rowCount() {
        return pageEntries().size() + (totalPages() > 1 ? 1 : 0);
    }

    private int maxScroll() {
        return Math.max(0, rowCount() * ROW_HEIGHT - listHeight());
    }

    private void clampScroll() {
        page = Math.min(page, totalPages() - 1);
        scrollOffset = Math.clamp(scrollOffset, 0, maxScroll());
    }

    private List<RowText> rowTexts() {
        String key = LeaderboardClient.version() + "/" + mode + "/" + page + "/" + panelWidth();
        if (!key.equals(rowsKey)) {
            clampScroll();
            rowsKey = key;
            int nameX = AVATAR_COLUMN_X + AVATAR_SIZE + 6;
            int nameRight = panelWidth() - (mode == Mode.PLAYERS ? COL_KILLS + 28 : COL_KD + 40);
            int nameWidth = Math.max(20, nameRight - nameX);
            List<LeaderboardRow> entries = pageEntries();
            List<RowText> built = new ArrayList<>(entries.size());
            for (int i = 0; i < entries.size(); i++) {
                LeaderboardRow row = entries.get(i);
                String name = fit(row.name(), nameWidth);
                String rank = "#" + (page * PAGE_SIZE + i + 1);
                if (mode == Mode.PLAYERS) {
                    double kd = row.deaths() == 0 ? row.kills() : (double) row.kills() / row.deaths();
                    built.add(new RowText(row, rank, name, String.valueOf(row.kills()), String.valueOf(row.deaths()),
                        String.format(Locale.ROOT, "%.2f", kd)));
                } else {
                    built.add(new RowText(row, rank, name, String.valueOf(row.mobKills()), "", ""));
                }
            }
            rows = built;
        }
        return rows;
    }

    /** The text, cut with an ellipsis if it is wider than {@code maxWidth}. */
    private String fit(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        return font.plainSubstrByWidth(text, maxWidth - font.width("…")) + "…";
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        List<RowText> texts = rowTexts();

        int px = panelX();
        int py = panelY();
        int pw = panelWidth();
        int ph = panelHeight();

        HudRender.roundedFill(graphics, px, py, pw, ph, 6, 0xE6101014);
        graphics.centeredText(font, title, px + pw / 2, py + 8, 0xFFFFFFFF);

        int closeX = px + pw - 18;
        int closeY = py + 7;
        boolean closeHovered = inside(mouseX, mouseY, closeX, closeY, 12, 12);
        graphics.text(font, "x", closeX + 3, closeY + 2, closeHovered ? 0xFFFFFFFF : 0xFF999999);

        renderTab(graphics, mouseX, mouseY, tabRect(0), pvpTab, Mode.PLAYERS);
        renderTab(graphics, mouseX, mouseY, tabRect(1), mobTab, Mode.MOB_KILLS);

        int headerY = py + HEADER_HEIGHT - 20;
        graphics.text(font, rankLabel, px + 12, headerY, HEADER_COLOR);
        graphics.text(font, playerLabel, px + AVATAR_COLUMN_X, headerY, HEADER_COLOR);
        if (mode == Mode.PLAYERS) {
            rightText(graphics, killsLabel, px + pw - COL_KILLS, headerY, HEADER_COLOR);
            rightText(graphics, deathsLabel, px + pw - COL_DEATHS, headerY, HEADER_COLOR);
            rightText(graphics, kdLabel, px + pw - COL_KD, headerY, HEADER_COLOR);
        } else {
            rightText(graphics, mobTab, px + pw - COL_KD, headerY, HEADER_COLOR);
        }
        graphics.fill(px + 8, py + HEADER_HEIGHT - 4, px + pw - 8, py + HEADER_HEIGHT - 3, 0x40FFFFFF);

        prevButtonRect = null;
        nextButtonRect = null;
        if (texts.isEmpty()) {
            String key = switch (LeaderboardClient.state()) {
                case LOADING -> "screen.krylix.leaderboard.loading";
                case UNAVAILABLE -> "screen.krylix.leaderboard.unavailable";
                default -> mode == Mode.PLAYERS ? "screen.krylix.leaderboard.empty_players" : "screen.krylix.leaderboard.empty_mob_kills";
            };
            graphics.centeredText(font, Component.translatable(key), px + pw / 2, listTop() + listHeight() / 2 - 4, 0xFFAAAAAA);
        } else {
            renderRows(graphics, texts, mouseX, mouseY);
        }

        graphics.centeredText(font, Component.translatable("krylix.leaderboard.footer", LeaderboardClient.totalPlayers()),
            px + pw / 2, py + ph - 14, 0xFF777777);
    }

    private int[] tabRect(int index) {
        int gap = 6;
        int startX = panelX() + (panelWidth() - (TAB_WIDTH * 2 + gap)) / 2;
        return new int[]{startX + index * (TAB_WIDTH + gap), panelY() + 20, TAB_WIDTH, TAB_HEIGHT};
    }

    private void renderTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int[] rect, String label, Mode tabMode) {
        boolean active = mode == tabMode;
        boolean hovered = inside(mouseX, mouseY, rect);
        int bg = active ? 0xCC3A7BD5 : (hovered ? 0x30FFFFFF : 0x20FFFFFF);
        HudRender.roundedFill(graphics, rect[0], rect[1], rect[2], rect[3], 3, bg);
        graphics.centeredText(font, label, rect[0] + rect[2] / 2, rect[1] + (rect[3] - 8) / 2, active ? 0xFFFFFFFF : 0xFFAAAAAA);
    }

    private void renderRows(GuiGraphicsExtractor graphics, List<RowText> texts, int mouseX, int mouseY) {
        int px = panelX();
        int pw = panelWidth();
        int top = listTop();
        int bottom = listBottom();

        graphics.enableScissor(px + 6, top, px + pw - 6, bottom);
        UUID self = minecraft != null && minecraft.player != null ? minecraft.player.getUUID() : null;
        boolean mouseInList = mouseY >= top && mouseY < bottom;

        int y = top - scrollOffset;
        for (int i = 0; i < texts.size(); i++) {
            if (y + ROW_HEIGHT >= top && y <= bottom) {
                RowText text = texts.get(i);
                boolean hovered = mouseInList && inside(mouseX, mouseY, px + 6, y, pw - 12, ROW_HEIGHT);
                int bg = hovered ? 0x33FFFFFF : text.row().uuid().equals(self) ? 0x2255AAFF : i % 2 == 0 ? 0x18FFFFFF : 0;
                if (bg != 0) {
                    graphics.fill(px + 6, y, px + pw - 6, y + ROW_HEIGHT, bg);
                }
                renderRow(graphics, text, px, pw, y);
            }
            y += ROW_HEIGHT;
        }
        if (totalPages() > 1 && y + ROW_HEIGHT >= top && y <= bottom) {
            renderPaginationRow(graphics, mouseX, mouseY, px, pw, y);
        }
        graphics.disableScissor();
    }

    private void renderRow(GuiGraphicsExtractor graphics, RowText text, int px, int pw, int y) {
        int textY = y + (ROW_HEIGHT - 8) / 2;
        graphics.text(font, text.rank(), px + RANK_COLUMN_END - font.width(text.rank()), textY, 0xFFAAAAAA);
        int avatarX = px + AVATAR_COLUMN_X;
        Avatars.draw(graphics, text.row().uuid(), null, avatarX, y + (ROW_HEIGHT - AVATAR_SIZE) / 2, AVATAR_SIZE);
        graphics.text(font, text.name(), avatarX + AVATAR_SIZE + 6, textY, 0xFFFFFFFF);
        if (mode == Mode.PLAYERS) {
            rightText(graphics, text.first(), px + pw - COL_KILLS, textY, 0xFFFF8080);
            rightText(graphics, text.second(), px + pw - COL_DEATHS, textY, 0xFFCCCCCC);
            rightText(graphics, text.third(), px + pw - COL_KD, textY, 0xFFFFD700);
        } else {
            rightText(graphics, text.first(), px + pw - COL_KD, textY, 0xFFCCCCCC);
        }
    }

    private void renderPaginationRow(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int px, int pw, int y) {
        int btnSize = 16;
        int gap = 8;
        Component pageText = Component.translatable("krylix.leaderboard.page", page + 1, totalPages());
        int pageTextWidth = font.width(pageText);
        int startX = px + (pw - (btnSize + gap + pageTextWidth + gap + btnSize)) / 2;
        int btnY = y + (ROW_HEIGHT - btnSize) / 2;
        // Clickable only while the buttons are inside the list, not scrolled under the footer
        if (btnY >= listTop() && btnY + btnSize <= listBottom()) {
            prevButtonRect = new int[]{startX, btnY, btnSize, btnSize};
            nextButtonRect = new int[]{startX + btnSize + gap + pageTextWidth + gap, btnY, btnSize, btnSize};
        }
        renderPageButton(graphics, mouseX, mouseY, startX, btnY, btnSize, false, page > 0);
        renderPageButton(graphics, mouseX, mouseY, startX + btnSize + gap + pageTextWidth + gap, btnY, btnSize, true, page < totalPages() - 1);
        graphics.text(font, pageText, startX + btnSize + gap, y + (ROW_HEIGHT - 8) / 2, 0xFFCCCCCC);
    }

    private void renderPageButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, int size, boolean pointRight, boolean enabled) {
        boolean hovered = enabled && inside(mouseX, mouseY, x, y, size, size);
        HudRender.roundedFill(graphics, x, y, size, size, 3, !enabled ? 0x15FFFFFF : hovered ? 0x40FFFFFF : 0x25FFFFFF);
        int color = enabled ? 0xFFFFFFFF : 0xFF555555;
        int arrowW = 4;
        int arrowH = 7;
        int originX = x + (size - arrowW) / 2;
        int originY = y + (size - arrowH) / 2;
        int mid = arrowH / 2;
        for (int row = 0; row < arrowH; row++) {
            int length = Math.max(1, arrowW - Math.abs(row - mid));
            int rowY = originY + row;
            if (pointRight) {
                graphics.fill(originX, rowY, originX + length, rowY + 1, color);
            } else {
                graphics.fill(originX + (arrowW - length), rowY, originX + arrowW, rowY + 1, color);
            }
        }
    }

    private void rightText(GuiGraphicsExtractor graphics, String text, int rightX, int y, int color) {
        graphics.text(font, text, rightX - font.width(text), y, color);
    }

    private static boolean inside(double mouseX, double mouseY, int[] rect) {
        return rect != null && inside(mouseX, mouseY, rect[0], rect[1], rect[2], rect[3]);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private void changePage(int delta) {
        int next = Math.clamp(page + delta, 0, totalPages() - 1);
        if (next != page) {
            page = next;
            scrollOffset = 0;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset = Math.clamp(scrollOffset - (int) (scrollY * ROW_HEIGHT * 1.5), 0, maxScroll());
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        double mouseX = event.x();
        double mouseY = event.y();
        if (inside(mouseX, mouseY, panelX() + panelWidth() - 18, panelY() + 7, 12, 12)) {
            onClose();
            return true;
        }
        if (inside(mouseX, mouseY, tabRect(0))) {
            switchTo(Mode.PLAYERS);
            return true;
        }
        if (inside(mouseX, mouseY, tabRect(1))) {
            switchTo(Mode.MOB_KILLS);
            return true;
        }
        if (inside(mouseX, mouseY, prevButtonRect)) {
            changePage(-1);
            return true;
        }
        if (inside(mouseX, mouseY, nextButtonRect)) {
            changePage(1);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** Left / right: previous / next page; up / down: scroll; Tab key handling stays vanilla's. */
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isLeft()) {
            changePage(-1);
            return true;
        }
        if (event.isRight()) {
            changePage(1);
            return true;
        }
        if (event.isUp() || event.isDown()) {
            scrollOffset = Math.clamp(scrollOffset + (event.isUp() ? -ROW_HEIGHT : ROW_HEIGHT), 0, maxScroll());
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
