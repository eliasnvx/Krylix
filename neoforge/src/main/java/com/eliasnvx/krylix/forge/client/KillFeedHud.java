package com.eliasnvx.krylix.forge.client;

import com.eliasnvx.krylix.Krylix;
import com.eliasnvx.krylix.forge.config.KrylixConfig;
import com.eliasnvx.krylix.model.KillEntry;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
    private static final Map<String, ResourceLocation> offlineSkinCache = new HashMap<>();

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
        ResourceLocation id = ResourceLocation.tryParse(weaponId);
        if (id == null) return Items.IRON_SWORD.getDefaultInstance();
        Item item = BuiltInRegistries.ITEM.get(id);
        return item == Items.AIR ? Items.IRON_SWORD.getDefaultInstance() : item.getDefaultInstance();
    }

    public static void render(GuiGraphics guiGraphics, float partialTick) {
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

    private static void renderEntry(GuiGraphics guiGraphics, Font font, KillEntry entry, int screenWidth, int y) {
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

        guiGraphics.drawString(font, entry.getKillerName(), currentX, y + 4, getAlphaColor(killerColor, alpha));
        currentX += killerNameWidth + padding;

        ItemStack weaponItem = parseWeaponItem(entry.getWeaponName());
        int animationOffset = getWeaponAnimationOffset(entry.getTimestamp());
        guiGraphics.renderItem(weaponItem, currentX, y + animationOffset);
        currentX += weaponSize + padding;

        renderPlayerHead(guiGraphics, victimUUID, entry.getVictimName(), currentX, y, alpha);
        currentX += headSize + padding;

        guiGraphics.drawString(font, entry.getVictimName(), currentX, y + 4, getAlphaColor(victimColor, alpha));
        currentX += victimNameWidth + padding;

        guiGraphics.drawString(font, hpText, currentX, y + 4, getAlphaColor(hpColor, alpha));
        renderHeartIcon(guiGraphics, currentX + font.width(hpText) + 2, y + 1, alpha);
        currentX += hpWidth;

        if (!badgeText.isEmpty()) {
            int badgeColor;
            if (entry.isSmash()) badgeColor = new Color(255, 130, 40).getRGB();
            else if (entry.isLongshot()) badgeColor = new Color(80, 220, 200).getRGB();
            else if (entry.isCritical()) badgeColor = new Color(255, 215, 0).getRGB();
            else badgeColor = new Color(180, 180, 180).getRGB();
            
            guiGraphics.drawString(font, badgeText, currentX + padding, y + 4, getAlphaColor(badgeColor, alpha));
        }
    }

    private static void renderSelfKillEntry(GuiGraphics guiGraphics, Font font, KillEntry entry, int screenWidth, int y, float alpha) {
        UUID victimUUID = entry.getVictimUUIDString() != null ? UUID.fromString(entry.getVictimUUIDString()) : null;
        int victimNameWidth = font.width(entry.getVictimName());
        int totalWidth = headSize + padding + victimNameWidth + padding + weaponSize;
        int startX = screenWidth - totalWidth - 10;

        int currentX = startX;

        renderPlayerHead(guiGraphics, victimUUID, entry.getVictimName(), currentX, y, alpha);
        currentX += headSize + padding;

        guiGraphics.drawString(font, entry.getVictimName(), currentX, y + 4, getAlphaColor(victimColor, alpha));
        currentX += victimNameWidth + padding;

        ItemStack weaponItem = parseWeaponItem(entry.getWeaponName());
        int animationOffset = getWeaponAnimationOffset(entry.getTimestamp());
        guiGraphics.renderItem(weaponItem, currentX, y + animationOffset);
    }

    private static void renderPlayerHead(GuiGraphics guiGraphics, UUID playerUUID, String playerName, int x, int y, float alpha) {
        Minecraft minecraft = Minecraft.getInstance();
        ResourceLocation skinTexture = null;
        boolean isMob = false;

        if (playerUUID != null && minecraft.getConnection() != null) {
            PlayerInfo playerInfo = minecraft.getConnection().getPlayerInfo(playerUUID);
            if (playerInfo != null) skinTexture = playerInfo.getSkin().texture();
        }

        if (skinTexture == null && playerName != null && minecraft.getConnection() != null) {
            for (PlayerInfo playerInfo : minecraft.getConnection().getOnlinePlayers()) {
                if (playerInfo.getProfile().getName().equalsIgnoreCase(playerName)) {
                    skinTexture = playerInfo.getSkin().texture();
                    break;
                }
            }
            if (skinTexture == null) {
                skinTexture = MobTextures.byDisplayName(playerName);
                if (skinTexture != null) {
                    isMob = true;
                } else if (playerUUID != null) {
                    skinTexture = offlineSkinCache.computeIfAbsent(playerUUID.toString(), k -> {
                        try {
                            return minecraft.getSkinManager().getInsecureSkin(new GameProfile(playerUUID, playerName)).texture();
                        } catch (Exception e) {
                            return null;
                        }
                    });
                }
            }
        }

        if (skinTexture != null) {
            try {
                RenderSystem.setShaderTexture(0, skinTexture);
                RenderSystem.enableBlend();

                boolean finalIsMob = isMob;
                ResourceLocation finalSkinTexture = skinTexture;
                HudRender.rounded(guiGraphics, x, y, headSize, avatarCornerCut, () -> {
                    if (finalIsMob) {
                        MobTextures.blitMobFace(guiGraphics, finalSkinTexture, MobTextures.guessEntityId(playerName), x, y, headSize);
                    } else {
                        guiGraphics.blit(finalSkinTexture, x, y, headSize, headSize, 8.0f, 8.0f, 8, 8, 64, 64);
                        guiGraphics.blit(finalSkinTexture, x, y, headSize, headSize, 40.0f, 8.0f, 8, 8, 64, 64);
                    }
                });
                RenderSystem.disableBlend();
            } catch (Exception e) {
                renderFallbackHead(guiGraphics, playerName, x, y, alpha);
            }
        } else {
            renderFallbackHead(guiGraphics, playerName, x, y, alpha);
        }
    }

    private static void renderFallbackHead(GuiGraphics guiGraphics, String playerName, int x, int y, float alpha) {
        String name = playerName != null ? playerName : "Unknown";
        int color = name.toLowerCase().contains("1") ? killerColor : victimColor;
        HudRender.rounded(guiGraphics, x, y, headSize, avatarCornerCut, () -> {
            guiGraphics.fill(x, y, x + headSize, y + headSize, getAlphaColor(color, alpha));
        });
    }

    private static void renderHeartIcon(GuiGraphics guiGraphics, int x, int y, float alpha) {
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        guiGraphics.blitSprite(ResourceLocation.fromNamespaceAndPath("minecraft", "hud/heart/full"), x, y, heartSize, heartSize);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
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
        com.eliasnvx.krylix.forge.config.KrylixConfig.save();
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
