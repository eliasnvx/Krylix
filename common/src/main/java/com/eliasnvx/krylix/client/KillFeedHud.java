package com.eliasnvx.krylix.client;

import com.eliasnvx.krylix.config.KrylixConfig;
import com.eliasnvx.krylix.model.KillEntry;
import com.eliasnvx.krylix.network.KrylixPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.Deque;

/** The kill feed in the top-right corner. Client thread only. */
public final class KillFeedHud {
    private static final Deque<Row> rows = new ArrayDeque<>();

    private static final int ENTRY_HEIGHT = 16;
    private static final int HEAD = 12;
    private static final int WEAPON = 12;
    private static final int PAD = 4;
    private static final int SPACING = 4;
    private static final int HEART = 9;
    private static final int MARGIN = 10;

    private static final int KILLER_COLOR = 0xFFFF6464;
    private static final int VICTIM_COLOR = 0xFF6496FF;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final Identifier HEART_SPRITE = Identifier.withDefaultNamespace("hud/heart/full");

    /** A feed row with everything the renderer needs worked out once, when it arrives. */
    private record Row(KillEntry entry, String killerHp, ItemStack weapon, String badge, int badgeColor) {
        static Row of(KillEntry entry) {
            String badge = "";
            int color = 0xFFB4B4B4;
            if (entry.has(KrylixPayloads.FLAG_SMASH)) {
                badge = "🔨";
                color = 0xFFFF8228;
            } else if (entry.has(KrylixPayloads.FLAG_LONGSHOT)) {
                badge = "🎯 " + (int) entry.distance() + "m";
                color = 0xFF50DCC8;
            } else if (entry.has(KrylixPayloads.FLAG_CRITICAL)) {
                badge = "⚡";
                color = 0xFFFFD700;
            } else if (entry.hasDistance() && entry.distance() >= 10) {
                badge = (int) entry.distance() + "m";
            }
            return new Row(entry, String.valueOf(Math.round(entry.killerHealth())), Avatars.item(entry.weapon()), badge, color);
        }
    }

    private KillFeedHud() {
    }

    public static void add(KillEntry entry) {
        if (!isHudEnabled()) {
            return;
        }
        rows.addLast(Row.of(entry));
        while (rows.size() > Math.max(1, KrylixConfig.get().maxEntries)) {
            rows.removeFirst();
        }
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.level().playSound(player, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.2f);
        }
    }

    public static void render(GuiGraphicsExtractor graphics) {
        if (!isHudEnabled() || rows.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int displaySeconds = KrylixConfig.get().displaySeconds;
        long now = System.currentTimeMillis();
        rows.removeIf(row -> row.entry().isExpired(displaySeconds, now));

        int y = 5;
        for (Row row : rows) {
            float alpha = row.entry().alpha(displaySeconds, now);
            if (row.entry().isEnvironmentalDeath() || row.entry().isSuicide()) {
                renderSolo(graphics, font, row, screenWidth, y, alpha, now);
            } else {
                renderKill(graphics, font, row, screenWidth, y, alpha, now);
            }
            y += ENTRY_HEIGHT + SPACING;
        }
    }

    private static void renderKill(GuiGraphicsExtractor graphics, Font font, Row row, int screenWidth, int y, float alpha, long now) {
        KillEntry entry = row.entry();
        String killerName = entry.killer().name();
        String victimName = entry.victim().name();
        int badgeWidth = row.badge().isEmpty() ? 0 : font.width(row.badge()) + PAD;
        int hpWidth = font.width(row.killerHp()) + 2 + HEART;
        int total = HEAD + PAD + font.width(killerName) + PAD + WEAPON + PAD + HEAD + PAD + font.width(victimName) + PAD + hpWidth + badgeWidth;
        int x = screenWidth - total - MARGIN;

        Avatars.draw(graphics, entry.killer(), x, y, HEAD);
        x += HEAD + PAD;
        graphics.text(font, killerName, x, y + 4, withAlpha(KILLER_COLOR, alpha));
        x += font.width(killerName) + PAD;
        drawWeapon(graphics, font, row, x, y, alpha, now);
        x += WEAPON + PAD;
        Avatars.draw(graphics, entry.victim(), x, y, HEAD);
        x += HEAD + PAD;
        graphics.text(font, victimName, x, y + 4, withAlpha(VICTIM_COLOR, alpha));
        x += font.width(victimName) + PAD;
        graphics.text(font, row.killerHp(), x, y + 4, withAlpha(TEXT_COLOR, alpha));
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_SPRITE, x + font.width(row.killerHp()) + 2, y + 1, HEART, HEART);
        x += hpWidth;
        if (!row.badge().isEmpty()) {
            graphics.text(font, row.badge(), x + PAD, y + 4, withAlpha(row.badgeColor(), alpha));
        }
    }

    /** Falls, lava, self-kills: the victim and what got them. */
    private static void renderSolo(GuiGraphicsExtractor graphics, Font font, Row row, int screenWidth, int y, float alpha, long now) {
        KillEntry entry = row.entry();
        String victimName = entry.victim().name();
        int total = HEAD + PAD + font.width(victimName) + PAD + WEAPON;
        int x = screenWidth - total - MARGIN;
        Avatars.draw(graphics, entry.victim(), x, y, HEAD);
        x += HEAD + PAD;
        graphics.text(font, victimName, x, y + 4, withAlpha(VICTIM_COLOR, alpha));
        x += font.width(victimName) + PAD;
        drawWeapon(graphics, font, row, x, y, alpha, now);
    }

    private static void drawWeapon(GuiGraphicsExtractor graphics, Font font, Row row, int x, int y, float alpha, long now) {
        if (row.weapon().isEmpty()) {
            graphics.text(font, "»", x + 3, y + 4, withAlpha(TEXT_COLOR, alpha)); // bare hands / plain mob attack
            return;
        }
        long age = now - row.entry().receivedAt();
        int bob = (int) (2.0 * Math.sin(age / 1000.0 * 4.0 * Math.PI));
        graphics.item(row.weapon(), x, y + bob);
    }

    private static int withAlpha(int color, float alpha) {
        return (color & 0xFFFFFF) | ((int) (alpha * 255) << 24);
    }

    public static void setEnabled(boolean enabled) {
        KrylixConfig.get().killFeedEnabled = enabled;
        KrylixConfig.save();
        if (!enabled) {
            rows.clear();
        }
    }

    public static boolean isHudEnabled() {
        return KrylixConfig.get().killFeedEnabled;
    }

    public static int count() {
        return rows.size();
    }

    public static void clear() {
        rows.clear();
    }
}
