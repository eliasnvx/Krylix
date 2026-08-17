package com.eliasnvx.krylix.fabric.client;

import net.minecraft.client.renderer.RenderPipelines;
import com.eliasnvx.krylix.Krylix;
import com.eliasnvx.krylix.fabric.config.KrylixConfig;
import com.eliasnvx.krylix.model.KillEntry;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class KillFeedHud {
    private static final List<KillEntry> activeNotifications = new ArrayList<>();
    private static final Map<String, Identifier> offlineSkinCache = new HashMap<>();

    private static int getMaxEntries() { return KrylixConfig.get().maxEntries; }
    private static int getDisplaySeconds() { return KrylixConfig.get().displaySeconds; }

    private static final int entryHeight = 16;
    private static final int headSize = 12;
    private static final int weaponSize = 12;
    private static final int padding = 4;
    private static final int entrySpacing = 4;
    private static final int avatarCornerCut = 2;
    private static final int heartSize = 9;

    private static final int killerColor = new Color(255, 100, 100).getRGB();
    private static final int victimColor = new Color(100, 150, 255).getRGB();
    private static final int hpColor = new Color(255, 255, 255).getRGB();

    public static void addEntry(KillEntry killEntry) {
        if (!isHudEnabled()) return;

        activeNotifications.add(killEntry);
        if (activeNotifications.size() > getMaxEntries()) {
            activeNotifications.remove(0);
        }

        playKillSound();
        Krylix.LOGGER.debug("Added kill notification to HUD: " + killEntry.getKillerName() + " killed " + killEntry.getVictimName());
    }

    private static void playKillSound() {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) return;

        player.level().playSound(player, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.2f);
    }

    private static ItemStack parseWeaponItem(String weaponId) {
        if (weaponId == null) return Items.IRON_SWORD.getDefaultInstance();
        Identifier id = Identifier.tryParse(weaponId);
        if (id == null) return Items.IRON_SWORD.getDefaultInstance();
        Item item = BuiltInRegistries.ITEM.getValue(id);
        return item == Items.AIR ? Items.IRON_SWORD.getDefaultInstance() : item.getDefaultInstance();
    }

    public static void render(GuiGraphicsExtractor guiGraphics, float partialTick) {
        if (!isHudEnabled() || activeNotifications.isEmpty()) return;

        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();

        cleanupOldNotifications();

        int startY = 5;
        List<KillEntry> notificationsCopy = new ArrayList<>(activeNotifications);

        for (int i = 0; i < notificationsCopy.size(); i++) {
            KillEntry entry = notificationsCopy.get(i);
            int y = startY + (i * (entryHeight + entrySpacing));
            renderEntry(guiGraphics, font, entry, screenWidth, y);
        }
    }

    private static void renderEntry(GuiGraphicsExtractor guiGraphics, Font font, KillEntry entry, int screenWidth, int y) {
        float alpha = entry.getAlpha(getDisplaySeconds());

        if (entry.isEnvironmentalDeath() || entry.isSuicide()) {
            renderSelfKillEntry(guiGraphics, font, entry, screenWidth, y, alpha);
            return;
        }

        UUID killerUUID = entry.getKillerUUIDString() != null ? UUID.fromString(entry.getKillerUUIDString()) : null;
        UUID victimUUID = entry.getVictimUUIDString() != null ? UUID.fromString(entry.getVictimUUIDString()) : null;

        String badgeText = "";
        if (entry.isSmash()) badgeText = "🔨";
        else if (entry.isLongshot()) badgeText = "🎯 " + (entry.getDistance() != null ? entry.getDistance().intValue() : 0) + "m";
        else if (entry.isCritical()) badgeText = "⚡";
        else if (entry.getDistance() != null && entry.getDistance() >= 10.0) badgeText = entry.getDistance().intValue() + "m";

        int badgeWidth = !badgeText.isEmpty() ? font.width(badgeText) + padding : 0;

        String hpText = String.valueOf((int) entry.getKillerHealth());
        int hpWidth = font.width(hpText) + 2 + heartSize;
        int killerNameWidth = font.width(entry.getKillerName());
        int victimNameWidth = font.width(entry.getVictimName());

        int totalWidth = headSize + padding + killerNameWidth + padding +
                weaponSize + padding + headSize + padding +
                victimNameWidth + padding + hpWidth + badgeWidth;

        int currentX = screenWidth - totalWidth - 10;

        renderPlayerHead(guiGraphics, killerUUID, entry.getKillerName(), currentX, y, alpha);
        currentX += headSize + padding;

        guiGraphics.text(font, entry.getKillerName(), currentX, y + 4, getAlphaColor(killerColor, alpha));
        currentX += killerNameWidth + padding;

        ItemStack weaponItem = parseWeaponItem(entry.getWeaponName());
        int animationOffset = getWeaponAnimationOffset(entry.getTimestamp());
        guiGraphics.item(weaponItem, currentX, y + animationOffset);
        currentX += weaponSize + padding;

        renderPlayerHead(guiGraphics, victimUUID, entry.getVictimName(), currentX, y, alpha);
        currentX += headSize + padding;

        guiGraphics.text(font, entry.getVictimName(), currentX, y + 4, getAlphaColor(victimColor, alpha));
        currentX += victimNameWidth + padding;

        guiGraphics.text(font, hpText, currentX, y + 4, getAlphaColor(hpColor, alpha));
        renderHeartIcon(guiGraphics, currentX + font.width(hpText) + 2, y + 1, alpha);
        currentX += hpWidth;

        if (!badgeText.isEmpty()) {
            int badgeColor;
            if (entry.isSmash()) badgeColor = new Color(255, 130, 40).getRGB();
            else if (entry.isLongshot()) badgeColor = new Color(80, 220, 200).getRGB();
            else if (entry.isCritical()) badgeColor = new Color(255, 215, 0).getRGB();
            else badgeColor = new Color(180, 180, 180).getRGB();
            
            guiGraphics.text(font, badgeText, currentX + padding, y + 4, getAlphaColor(badgeColor, alpha));
        }
    }

    private static void renderSelfKillEntry(GuiGraphicsExtractor guiGraphics, Font font, KillEntry entry, int screenWidth, int y, float alpha) {
        UUID victimUUID = entry.getVictimUUIDString() != null ? UUID.fromString(entry.getVictimUUIDString()) : null;
        int victimNameWidth = font.width(entry.getVictimName());
        int totalWidth = headSize + padding + victimNameWidth + padding + weaponSize;
        int startX = screenWidth - totalWidth - 10;

        int currentX = startX;

        renderPlayerHead(guiGraphics, victimUUID, entry.getVictimName(), currentX, y, alpha);
        currentX += headSize + padding;

        guiGraphics.text(font, entry.getVictimName(), currentX, y + 4, getAlphaColor(victimColor, alpha));
        currentX += victimNameWidth + padding;

        ItemStack weaponItem = parseWeaponItem(entry.getWeaponName());
        int animationOffset = getWeaponAnimationOffset(entry.getTimestamp());
        guiGraphics.item(weaponItem, currentX, y + animationOffset);
    }

    private static void renderPlayerHead(GuiGraphicsExtractor guiGraphics, UUID playerUUID, String playerName, int x, int y, float alpha) {
        Minecraft minecraft = Minecraft.getInstance();
        Identifier skinTexture = null;
        boolean isMob = false;

        if (playerUUID != null && minecraft.getConnection() != null) {
            PlayerInfo playerInfo = minecraft.getConnection().getPlayerInfo(playerUUID);
            if (playerInfo != null && playerInfo.getSkin() != null) skinTexture = playerInfo.getSkin().body().texturePath();
        }

        if (skinTexture == null && playerName != null && minecraft.getConnection() != null) {
            for (PlayerInfo playerInfo : minecraft.getConnection().getOnlinePlayers()) {
                if (playerInfo.getProfile().name().equalsIgnoreCase(playerName)) {
                    if (playerInfo.getSkin() != null) {
                        skinTexture = playerInfo.getSkin().body().texturePath();
                    }
                    break;
                }
            }
            if (skinTexture == null) {
                skinTexture = MobTextures.byDisplayName(playerName);
                if (skinTexture != null) {
                    isMob = true;
                } else if (playerUUID != null) {
                    skinTexture = net.minecraft.client.resources.DefaultPlayerSkin.get(playerUUID).body().texturePath();
                }
            }
        }

        if (skinTexture != null) {
            try {
                if (isMob) {
                    MobTextures.blitMobFace(guiGraphics, skinTexture, MobTextures.guessEntityId(playerName), x, y, headSize);
                } else {
                    net.minecraft.client.gui.components.PlayerFaceExtractor.extractRenderState(guiGraphics, skinTexture, x, y, headSize, true, false, -1);
                }
            } catch (Exception e) {
                renderFallbackHead(guiGraphics, playerName, x, y, alpha);
            }
        } else {
            renderFallbackHead(guiGraphics, playerName, x, y, alpha);
        }
    }

    private static void renderFallbackHead(GuiGraphicsExtractor guiGraphics, String playerName, int x, int y, float alpha) {
        String name = playerName != null ? playerName : "Unknown";
        int color = name.toLowerCase().contains("1") ? killerColor : victimColor;
        HudRender.roundedFill(guiGraphics, x, y, headSize, headSize, avatarCornerCut, getAlphaColor(color, alpha));
    }

    private static void renderHeartIcon(GuiGraphicsExtractor guiGraphics, int x, int y, float alpha) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.fromNamespaceAndPath("minecraft", "hud/heart/full"), x, y, heartSize, heartSize);
    }

    private static int getAlphaColor(int baseColor, float alpha) {
        int alphaInt = (int) (alpha * 255);
        return (baseColor & 0xFFFFFF) | (alphaInt << 24);
    }

    private static int getWeaponAnimationOffset(long timestamp) {
        long age = System.currentTimeMillis() - timestamp;
        double frequency = 2.0;
        double amplitude = 2.0;
        return (int) (amplitude * Math.sin(age / 1000.0 * frequency * 2.0 * Math.PI));
    }

    private static void cleanupOldNotifications() {
        int displaySeconds = getDisplaySeconds();
        activeNotifications.removeIf(entry -> entry.isExpired(displaySeconds));
    }

    public static void setEnabled(boolean enabled) {
        KrylixConfig.get().killFeedEnabled = enabled;
        KrylixConfig.save();
        if (!enabled) activeNotifications.clear();
        Krylix.LOGGER.info("Kill feed HUD " + (enabled ? "enabled" : "disabled"));
    }

    public static boolean isHudEnabled() {
        return KrylixConfig.get().killFeedEnabled;
    }

    public static int getNotificationCount() {
        return activeNotifications.size();
    }

    public static void clearNotifications() {
        activeNotifications.clear();
        offlineSkinCache.clear();
        Krylix.LOGGER.info("Cleared all kill notifications");
    }
}
