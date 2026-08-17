package com.eliasnvx.krylix.forge.client;

import net.minecraft.client.renderer.RenderPipelines;
import com.eliasnvx.krylix.Krylix;
import com.eliasnvx.krylix.forge.network.NetworkPackets.DeathRecapPacket;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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

    private static final Map<String, Identifier> skinCache = new HashMap<>();

    public static void setRecap(DeathRecapPacket recap) {
        activeRecap = recap;
        recapTimestamp = System.currentTimeMillis();
        Krylix.LOGGER.debug("Received Death Recap: " + recap);
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof DeathScreen screen) {
            DeathRecapPacket recap = activeRecap;
            if (recap == null) return;
            if (System.currentTimeMillis() - recapTimestamp > 300_000) return;

            render(event.getGuiGraphics(), screen.width, screen.height);
        }
    }

    private static void render(GuiGraphicsExtractor guiGraphics, int screenWidth, int screenHeight) {
        DeathRecapPacket recap = activeRecap;
        if (recap == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;

        int cardWidth = 240;
        int cardHeight = 72;
        int cardX = (screenWidth - cardWidth) / 2;
        int cardY = (screenHeight / 4) - 20;

        HudRender.roundedFill(guiGraphics, cardX, cardY, cardWidth, cardHeight, 6, 0xCC111116);

        String titleText = Component.translatable("krylix.deathrecap.title").getString();
        guiGraphics.centeredText(font, titleText, cardX + cardWidth / 2, cardY + 8, 0xFFFF5555);

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
        guiGraphics.text(font, recap.killerName(), textX, avatarY + 2, 0xFFFFFFFF);

        ItemStack weaponItem = parseWeaponItem(recap.weaponName());
        guiGraphics.item(weaponItem, textX + nameWidth + 6, avatarY - 3);

        guiGraphics.text(font, hpText, textX, avatarY + 16, 0xFFFFFFFF);
        guiGraphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                Identifier.fromNamespaceAndPath("minecraft", "hud/heart/full"),
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
        guiGraphics.text(font, damageDealtText, bottomStartX, cardY + cardHeight - 14, 0xFFFFAA00);

        if (!distText.isEmpty()) {
            guiGraphics.text(font, distText, bottomStartX + font.width(damageDealtText), cardY + cardHeight - 14, 0xFFAAAAAA);
        }

        String badgeText = "";
        int badgeColor = 0xFFFFD700;
        if (recap.isSmash()) {
            badgeText = "🔨 SMASH";
            badgeColor = 0xFFFF8822;
        } else if (recap.isLongshot()) {
            badgeText = "🎯 LONGSHOT";
            badgeColor = 0xFF50DCC8;
        } else if (recap.isCritical()) {
            badgeText = "⚡ CRIT";
            badgeColor = 0xFFFFD700;
        }

        if (!badgeText.isEmpty()) {
            int bWidth = font.width(badgeText);
            guiGraphics.text(font, badgeText, cardX + cardWidth - bWidth - 12, cardY + 8, badgeColor);
        }
    }

    private static void renderKillerAvatar(GuiGraphicsExtractor guiGraphics, DeathRecapPacket recap, int x, int y, int size) {
        Minecraft minecraft = Minecraft.getInstance();
        UUID uuid = null;
        if (recap.killerUUIDString() != null) {
            try {
                uuid = UUID.fromString(recap.killerUUIDString());
            } catch (Exception ignored) {}
        }
        
        Identifier skinTexture = null;
        boolean isMob = false;

        if (uuid != null && minecraft.getConnection() != null) {
            PlayerInfo playerInfo = minecraft.getConnection().getPlayerInfo(uuid);
            if (playerInfo != null && playerInfo.getSkin() != null) skinTexture = playerInfo.getSkin().body().texturePath();
            
            if (skinTexture == null) {
                skinTexture = net.minecraft.client.resources.DefaultPlayerSkin.get(uuid).body().texturePath();
            }
        } else {
            skinTexture = MobTextures.byDisplayName(recap.killerName());
            if (skinTexture != null) isMob = true;
        }

        if (skinTexture != null) {
            if (isMob) {
                String entityId = MobTextures.guessEntityId(recap.killerName());
                MobTextures.blitMobFace(guiGraphics, skinTexture, entityId, x, y, size);
            } else {
                net.minecraft.client.gui.components.PlayerFaceExtractor.extractRenderState(guiGraphics, skinTexture, x, y, size, true, false, -1);
            }
        } else {
            HudRender.roundedFill(guiGraphics, x, y, size, size, 3, 0x80808080);
        }
    }

    private static ItemStack parseWeaponItem(String weaponId) {
        if (weaponId == null || weaponId.equals("minecraft:air")) return new ItemStack(Items.AIR);
        try {
            Identifier location = Identifier.parse(weaponId);
            Item item = BuiltInRegistries.ITEM.getValue(location);
            if (item != null && item != Items.AIR) return new ItemStack(item);
            return new ItemStack(Items.IRON_SWORD);
        } catch (Exception e) {
            return new ItemStack(Items.IRON_SWORD);
        }
    }
}
