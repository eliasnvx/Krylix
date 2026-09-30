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
import net.minecraft.util.Util;
import net.minecraft.world.effect.MobEffectInstance;
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
    private static final int TOP = 5;
    /** Vanilla's status effect icons: 24 px rows at y = 1 (beneficial) and y = 27 (harmful). */
    private static final int EFFECT_ROW = 26;

    private static final int KILLER_COLOR = 0xFFFF6464;
    private static final int VICTIM_COLOR = 0xFF6496FF;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final Identifier HEART_SPRITE = Identifier.withDefaultNamespace("hud/heart/full");

    /** At most one sound per this many ms: a wipe of 50 players is one ding, not 50. */
    private static final long SOUND_GAP_MS = 100;
    private static long lastSoundAt;

    /** A feed row with everything the renderer needs worked out once: on arrival, and again only if the screen's width changes. */
    private static final class Row {
        final KillEntry entry;
        final String killerHp;
        final ItemStack weapon;
        final String badge;
        final int badgeColor;
        int layoutFor = -1;
        String killerName = "";
        String victimName = "";
        int killerWidth;
        int victimWidth;
        int hpWidth;
        int badgeWidth;

        Row(KillEntry entry) {
            this.entry = entry;
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
            this.badge = badge;
            this.badgeColor = color;
            this.killerHp = String.valueOf(Math.round(entry.killerHealth()));
            this.weapon = Avatars.item(entry.weapon());
        }

        /** Names cut to fit the screen; measured once per screen width, not every frame. */
        void layout(Font font, int maxNameWidth) {
            if (layoutFor == maxNameWidth) {
                return;
            }
            layoutFor = maxNameWidth;
            killerName = entry.killer() != null ? fit(font, entry.killer().name(), maxNameWidth) : "";
            victimName = fit(font, entry.victim().name(), maxNameWidth);
            killerWidth = font.width(killerName);
            victimWidth = font.width(victimName);
            hpWidth = font.width(killerHp) + 2 + HEART;
            badgeWidth = badge.isEmpty() ? 0 : font.width(badge) + PAD;
        }
    }

    private KillFeedHud() {
    }

    public static void add(KillEntry entry) {
        if (!isHudEnabled()) {
            return;
        }
        rows.addLast(new Row(entry));
        while (rows.size() > Math.max(1, KrylixConfig.get().maxEntries)) {
            rows.removeFirst();
        }
        Player player = Minecraft.getInstance().player;
        long now = Util.getMillis();
        if (player != null && now - lastSoundAt >= SOUND_GAP_MS) {
            lastSoundAt = now;
            player.level().playSound(player, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.2f);
        }
    }

    public static void render(GuiGraphicsExtractor graphics) {
        if (rows.isEmpty() || !isHudEnabled()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int displaySeconds = KrylixConfig.get().displaySeconds;
        long now = Util.getMillis();
        rows.removeIf(row -> row.entry.isExpired(displaySeconds, now));
        // Two long names must still fit next to each other with the icons, even on a small window
        int maxNameWidth = Math.clamp(screenWidth / 5, 30, 120);

        int y = top(minecraft);
        for (Row row : rows) {
            row.layout(font, maxNameWidth);
            float alpha = row.entry.alpha(displaySeconds, now);
            if (row.entry.isEnvironmentalDeath() || row.entry.isSuicide()) {
                renderSolo(graphics, font, row, screenWidth, y, alpha, now);
            } else {
                renderKill(graphics, font, row, screenWidth, y, alpha, now);
            }
            y += ENTRY_HEIGHT + SPACING;
        }
    }

    /** Below vanilla's status effect icons when there are any, so the feed never covers them. */
    private static int top(Minecraft minecraft) {
        Player player = minecraft.player;
        if (player == null) {
            return TOP;
        }
        boolean beneficial = false;
        boolean harmful = false;
        for (MobEffectInstance effect : player.getActiveEffects()) {
            if (effect.showIcon()) {
                if (effect.getEffect().value().isBeneficial()) {
                    beneficial = true;
                } else {
                    harmful = true;
                }
            }
        }
        return harmful ? 1 + 2 * EFFECT_ROW + 2 : beneficial ? 1 + EFFECT_ROW + 2 : TOP;
    }

    private static void renderKill(GuiGraphicsExtractor graphics, Font font, Row row, int screenWidth, int y, float alpha, long now) {
        KillEntry entry = row.entry;
        int total = HEAD + PAD + row.killerWidth + PAD + WEAPON + PAD + HEAD + PAD + row.victimWidth + PAD + row.hpWidth + row.badgeWidth;
        int x = Math.max(MARGIN, screenWidth - total - MARGIN);
        int iconColor = white(alpha);

        Avatars.draw(graphics, entry.killer().uuid(), entry.killer().entityType(), x, y, HEAD, iconColor);
        x += HEAD + PAD;
        graphics.text(font, row.killerName, x, y + 4, withAlpha(KILLER_COLOR, alpha));
        x += row.killerWidth + PAD;
        drawWeapon(graphics, font, row, x, y, alpha, now);
        x += WEAPON + PAD;
        Avatars.draw(graphics, entry.victim().uuid(), entry.victim().entityType(), x, y, HEAD, iconColor);
        x += HEAD + PAD;
        graphics.text(font, row.victimName, x, y + 4, withAlpha(VICTIM_COLOR, alpha));
        x += row.victimWidth + PAD;
        graphics.text(font, row.killerHp, x, y + 4, withAlpha(TEXT_COLOR, alpha));
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_SPRITE, x + font.width(row.killerHp) + 2, y + 1, HEART, HEART, iconColor);
        x += row.hpWidth;
        if (!row.badge.isEmpty()) {
            graphics.text(font, row.badge, x + PAD, y + 4, withAlpha(row.badgeColor, alpha));
        }
    }

    /** Falls, lava, self-kills: the victim and what got them. */
    private static void renderSolo(GuiGraphicsExtractor graphics, Font font, Row row, int screenWidth, int y, float alpha, long now) {
        KillEntry entry = row.entry;
        int total = HEAD + PAD + row.victimWidth + PAD + WEAPON;
        int x = Math.max(MARGIN, screenWidth - total - MARGIN);
        Avatars.draw(graphics, entry.victim().uuid(), entry.victim().entityType(), x, y, HEAD, white(alpha));
        x += HEAD + PAD;
        graphics.text(font, row.victimName, x, y + 4, withAlpha(VICTIM_COLOR, alpha));
        x += row.victimWidth + PAD;
        drawWeapon(graphics, font, row, x, y, alpha, now);
    }

    private static void drawWeapon(GuiGraphicsExtractor graphics, Font font, Row row, int x, int y, float alpha, long now) {
        if (row.weapon.isEmpty()) {
            graphics.text(font, "»", x + 3, y + 4, withAlpha(TEXT_COLOR, alpha)); // bare hands / plain mob attack
            return;
        }
        if (alpha < 0.5f) {
            return; // items can't fade: hide them for the second half of the fade-out instead of popping at the end
        }
        long age = now - row.entry.receivedAt();
        int bob = (int) (2.0 * Math.sin(age / 1000.0 * 4.0 * Math.PI));
        graphics.item(row.weapon, x, y + bob);
    }

    private static String fit(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        return font.plainSubstrByWidth(text, maxWidth - font.width("…")) + "…";
    }

    private static int withAlpha(int color, float alpha) {
        return (color & 0xFFFFFF) | ((int) (alpha * 255) << 24);
    }

    private static int white(float alpha) {
        return withAlpha(0xFFFFFF, alpha);
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
