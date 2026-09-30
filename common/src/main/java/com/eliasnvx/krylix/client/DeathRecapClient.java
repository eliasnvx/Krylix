package com.eliasnvx.krylix.client;

import com.eliasnvx.krylix.network.KrylixPayloads;
import com.eliasnvx.krylix.network.KrylixPayloads.DeathRecapPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/** The card under the death screen's buttons. Kept until the player respawns or leaves. */
public final class DeathRecapClient {
    private static final Identifier HEART_SPRITE = Identifier.withDefaultNamespace("hud/heart/full");
    /** A recap younger than this is kept even if the player still looks alive: on NeoForge it arrives before the death. */
    private static final long GRACE_MS = 1_500;
    private static final int MAX_MESSAGE_LINES = 3;

    private static @Nullable DeathRecapPayload recap;
    private static long receivedAt;
    // Text worked out once per recap, not every frame
    private static String dealtText = "";
    private static String distanceText = "";
    private static String badge = "";
    private static int badgeColor;

    private DeathRecapClient() {
    }

    public static void set(DeathRecapPayload payload) {
        recap = payload;
        receivedAt = Util.getMillis();
        dealtText = Component.translatable("krylix.deathrecap.damage_dealt", String.format(Locale.ROOT, "%.1f", payload.damageDealt())).getString();
        distanceText = payload.distance() >= 3 ? " (" + (int) payload.distance() + "m)" : "";
        if ((payload.flags() & KrylixPayloads.FLAG_SMASH) != 0) {
            badge = "🔨 " + Component.translatable("krylix.deathrecap.badge.smash").getString();
            badgeColor = 0xFFFF8822;
        } else if ((payload.flags() & KrylixPayloads.FLAG_LONGSHOT) != 0) {
            badge = "🎯 " + Component.translatable("krylix.deathrecap.badge.longshot").getString();
            badgeColor = 0xFF50DCC8;
        } else if ((payload.flags() & KrylixPayloads.FLAG_CRITICAL) != 0) {
            badge = "⚡ " + Component.translatable("krylix.deathrecap.badge.crit").getString();
            badgeColor = 0xFFFFD700;
        } else {
            badge = "";
        }
    }

    /** The player respawned: they are alive with no death screen, and the recap isn't one that just arrived. */
    public static boolean isStale(boolean aliveWithoutDeathScreen) {
        return recap != null && aliveWithoutDeathScreen && Util.getMillis() - receivedAt > GRACE_MS;
    }

    public static void clear() {
        recap = null;
        badge = "";
    }

    public static boolean hasRecap() {
        return recap != null;
    }

    public static void render(GuiGraphicsExtractor graphics, int screenWidth, int screenHeight) {
        DeathRecapPayload r = recap;
        if (r == null) {
            return;
        }
        Font font = Minecraft.getInstance().font;

        int cardWidth = 240;
        int cardHeight = 72;
        int cardX = (screenWidth - cardWidth) / 2;
        // Under vanilla's Respawn / Title Screen buttons (height/4 + 72 and + 96, 20 tall): above them are "You Died!",
        // the death message and the score. Where the window is too short for the whole card, it shrinks from its top edge.
        int cardY = screenHeight / 4 + 124;
        float scale = Math.max(0.5f, Math.min(1f, (screenHeight - cardY - 6) / (float) cardHeight));
        graphics.pose().pushMatrix();
        graphics.pose().translate(screenWidth / 2f, cardY);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-screenWidth / 2f, -cardY);

        HudRender.roundedFill(graphics, cardX, cardY, cardWidth, cardHeight, 6, 0xCC111116);
        graphics.centeredText(font, Component.translatable("krylix.deathrecap.title").getString(), cardX + cardWidth / 2, cardY + 8, 0xFFFF5555);

        if (r.killer() == null) {
            // No living killer: the vanilla death message, wrapped under the title
            List<FormattedCharSequence> lines = font.split(Component.literal(r.deathMessage()), cardWidth - 24);
            for (int i = 0; i < Math.min(lines.size(), MAX_MESSAGE_LINES); i++) {
                graphics.text(font, lines.get(i), cardX + 12, cardY + 26 + i * 10, 0xFFFFFFFF, true);
            }
            graphics.pose().popMatrix();
            return;
        }

        int avatarSize = 28;
        String name = r.killer().name();
        int nameWidth = font.width(name);
        String hpText = Math.round(r.killerHealth()) + " / " + Math.round(r.killerMaxHealth());
        int hpWidth = font.width(hpText) + 14;
        int blockWidth = avatarSize + 12 + Math.max(nameWidth + 24, hpWidth);
        int avatarX = cardX + (cardWidth - blockWidth) / 2;
        int avatarY = cardY + 22;
        Avatars.draw(graphics, r.killer(), avatarX, avatarY, avatarSize);

        int textX = avatarX + avatarSize + 12;
        graphics.text(font, name, textX, avatarY + 2, 0xFFFFFFFF);
        ItemStack weapon = Avatars.item(r.weapon());
        if (!weapon.isEmpty()) {
            graphics.item(weapon, textX + nameWidth + 6, avatarY - 3);
        }
        graphics.text(font, hpText, textX, avatarY + 16, 0xFFFFFFFF);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_SPRITE, textX + font.width(hpText) + 3, avatarY + 15, 9, 9);

        String dealt = dealtText;
        String dist = distanceText;
        int bottomWidth = font.width(dealt) + font.width(dist);
        int bottomX = cardX + (cardWidth - bottomWidth) / 2;
        graphics.text(font, dealt, bottomX, cardY + cardHeight - 14, 0xFFFFAA00);
        if (!dist.isEmpty()) {
            graphics.text(font, dist, bottomX + font.width(dealt), cardY + cardHeight - 14, 0xFFAAAAAA);
        }

        if (!badge.isEmpty()) {
            graphics.text(font, badge, cardX + cardWidth - font.width(badge) - 12, cardY + 8, badgeColor);
        }
        graphics.pose().popMatrix();
    }
}
