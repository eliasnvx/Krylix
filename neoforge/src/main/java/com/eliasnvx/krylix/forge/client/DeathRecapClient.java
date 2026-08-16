package com.eliasnvx.krylix.forge.client;

import com.eliasnvx.krylix.Krylix;
import com.eliasnvx.krylix.forge.network.NetworkPackets.DeathRecapPacket;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeathRecapClient {
    private static DeathRecapPacket activeRecap = null;
    private static long recapTimestamp = 0;

    private static final Map<String, ResourceLocation> skinCache = new HashMap<>();

    public static void setRecap(DeathRecapPacket recap) {
        activeRecap = recap;
        recapTimestamp = System.currentTimeMillis();
        Krylix.LOGGER.debug("Received Death Recap: " + recap);
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Pre event) {
        if (event.getScreen() instanceof DeathScreen screen) {
            DeathRecapPacket recap = activeRecap;
            if (recap == null) return;
            if (System.currentTimeMillis() - recapTimestamp > 300_000) return;

            event.setCanceled(true);

            screen.renderBackground(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());

            render(event.getGuiGraphics(), screen.width, screen.height);

            for (Renderable renderable : screen.renderables) {
                renderable.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
            }
        }
    }

    private static void render(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        DeathRecapPacket recap = activeRecap;
        if (recap == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;

        int cardWidth = 240;
        int cardHeight = 72;
        int cardX = (screenWidth - cardWidth) / 2;
        int cardY = (screenHeight / 4) - 20;

        HudRender.rounded(guiGraphics, cardX, cardY, cardWidth, cardHeight, 6, () -> {
            guiGraphics.fill(cardX, cardY, cardX + cardWidth, cardY + cardHeight, 0xCC111116);
        });

        String titleText = Component.translatable("krylix.deathrecap.title").getString();
        guiGraphics.drawCenteredString(font, titleText, cardX + cardWidth / 2, cardY + 8, 0xFF5555);

        int avatarSize = 28;
        int nameWidth = font.width(recap.killerName());
        String hpText = (int) recap.killerHealth() + " / " + (int) recap.killerMaxHealth();
        int hpWidth = font.width(hpText) + 14;

        int maxTextWidth = Math.max(nameWidth + 24, hpWidth);
        int blockWidth = avatarSize + 12 + maxTextWidth;
        int blockX = cardX + (cardWidth - blockWidth) / 2;

        int avatarX = blockX;
        int avatarY = cardY + 22;
        renderKillerAvatar(guiGraphics, recap, avatarX, avatarY, avatarSize);

        int textX = avatarX + avatarSize + 12;
        guiGraphics.drawString(font, recap.killerName(), textX, avatarY + 2, 0xFFFFFF);

        ItemStack weaponItem = parseWeaponItem(recap.weaponName());
        guiGraphics.renderItem(weaponItem, textX + nameWidth + 6, avatarY - 3);

        guiGraphics.drawString(font, hpText, textX, avatarY + 16, 0xFFFFFF);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        guiGraphics.blitSprite(
                ResourceLocation.fromNamespaceAndPath("minecraft", "hud/heart/full"),
                textX + font.width(hpText) + 3,
                avatarY + 15,
                9,
                9
        );

        String damageDealtText = Component.translatable("krylix.deathrecap.damage_dealt", String.format("%.1f", recap.damageDealtToKiller())).getString();
        int fullBottomWidth = font.width(damageDealtText);
        String distText = "";
        if (recap.distance() != null && recap.distance() >= 3.0) {
            distText = " (" + recap.distance().intValue() + "m)";
            fullBottomWidth += font.width(distText);
        }

        int bottomStartX = cardX + (cardWidth - fullBottomWidth) / 2;
        guiGraphics.drawString(font, damageDealtText, bottomStartX, cardY + cardHeight - 14, 0xFFAA00);

        if (!distText.isEmpty()) {
            guiGraphics.drawString(font, distText, bottomStartX + font.width(damageDealtText), cardY + cardHeight - 14, 0xAAAAAA);
        }

        String badgeText = "";
        int badgeColor = 0xFFD700;
        if (recap.isSmash()) {
            badgeText = "🔨 SMASH";
            badgeColor = 0xFF8822;
        } else if (recap.isLongshot()) {
            badgeText = "🎯 LONGSHOT";
            badgeColor = 0x50DCC8;
        } else if (recap.isCritical()) {
            badgeText = "⚡ CRIT";
            badgeColor = 0xFFD700;
        }

        if (!badgeText.isEmpty()) {
            int bWidth = font.width(badgeText);
            guiGraphics.drawString(font, badgeText, cardX + cardWidth - bWidth - 12, cardY + 8, badgeColor);
        }
    }

    private static void renderKillerAvatar(GuiGraphics guiGraphics, DeathRecapPacket recap, int x, int y, int size) {
        Minecraft minecraft = Minecraft.getInstance();
        UUID uuid = null;
        if (recap.killerUUIDString() != null) {
            try {
                uuid = UUID.fromString(recap.killerUUIDString());
            } catch (Exception ignored) {}
        }
        
        ResourceLocation skinTexture = null;
        boolean isMob = false;

        if (uuid != null && minecraft.getConnection() != null) {
            PlayerInfo playerInfo = minecraft.getConnection().getPlayerInfo(uuid);
            if (playerInfo != null) skinTexture = playerInfo.getSkin().texture();
            
            if (skinTexture == null) {
                UUID finalUuid = uuid;
                skinTexture = skinCache.computeIfAbsent(uuid.toString(), k -> {
                    try {
                        return minecraft.getSkinManager().getInsecureSkin(new GameProfile(finalUuid, recap.killerName())).texture();
                    } catch (Exception e) {
                        return null;
                    }
                });
            }
        } else {
            skinTexture = MobTextures.byDisplayName(recap.killerName());
            if (skinTexture != null) isMob = true;
        }

        ResourceLocation finalSkinTexture = skinTexture;
        boolean finalIsMob = isMob;
        HudRender.rounded(guiGraphics, x, y, size, 3, () -> {
            if (finalSkinTexture != null) {
                if (finalIsMob) {
                    String entityId = MobTextures.guessEntityId(recap.killerName());
                    MobTextures.blitMobFace(guiGraphics, finalSkinTexture, entityId, x, y, size);
                } else {
                    guiGraphics.blit(finalSkinTexture, x, y, size, size, 8.0f, 8.0f, 8, 8, 64, 64);
                }
            } else {
                guiGraphics.fill(x, y, x + size, y + size, 0x80808080);
            }
        });
    }

    private static ItemStack parseWeaponItem(String weaponId) {
        if (weaponId == null || weaponId.equals("minecraft:air")) return new ItemStack(Items.AIR);
        try {
            ResourceLocation location = ResourceLocation.parse(weaponId);
            Item item = BuiltInRegistries.ITEM.get(location);
            if (item != Items.AIR) return new ItemStack(item);
            return new ItemStack(Items.IRON_SWORD);
        } catch (Exception e) {
            return new ItemStack(Items.IRON_SWORD);
        }
    }
}
