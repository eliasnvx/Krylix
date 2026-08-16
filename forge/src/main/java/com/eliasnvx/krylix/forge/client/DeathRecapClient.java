package com.eliasnvx.krylix.forge.client;

import com.eliasnvx.krylix.forge.network.NetworkPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

public class DeathRecapClient {
    private static NetworkPackets.DeathRecapPacket currentRecap = null;

    public static void setRecap(NetworkPackets.DeathRecapPacket packet) {
        currentRecap = packet;
    }

    public static void clear() {
        currentRecap = null;
    }

    @SubscribeEvent
    public static void onRenderScreen(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof DeathScreen)) return;
        if (currentRecap == null) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int screenWidth = event.getScreen().width;
        int screenHeight = event.getScreen().height;

        int cardWidth = 240;
        int cardHeight = 85;
        int cardX = (screenWidth - cardWidth) / 2;
        int cardY = screenHeight / 4 + 40;

        guiGraphics.fill(cardX, cardY, cardX + cardWidth, cardY + cardHeight, 0xD0101015);
        guiGraphics.renderOutline(cardX, cardY, cardWidth, cardHeight, 0xFF4A1515);

        Component title = Component.translatable("krylix.deathrecap.title");
        int titleWidth = font.width(title);
        guiGraphics.drawString(font, title, cardX + (cardWidth - titleWidth) / 2, cardY + 6, 0xFFFF5555);

        int avatarSize = 28;
        int avatarX = cardX + 10;
        int avatarY = cardY + 22;

        boolean drawnAvatar = false;
        if (currentRecap.killerUUIDString() != null && !currentRecap.killerUUIDString().isEmpty()) {
            try {
                UUID uuid = UUID.fromString(currentRecap.killerUUIDString());
                PlayerSkin skin = minecraft.getSkinManager().getInsecureSkin(new com.mojang.authlib.GameProfile(uuid, currentRecap.killerName()));
                if (skin != null && skin.texture() != null) {
                    guiGraphics.blit(skin.texture(), avatarX, avatarY, avatarSize, avatarSize, 8.0f, 8.0f, 8, 8, 64, 64);
                    guiGraphics.blit(skin.texture(), avatarX, avatarY, avatarSize, avatarSize, 40.0f, 8.0f, 8, 8, 64, 64);
                    drawnAvatar = true;
                }
            } catch (Exception ignored) {}
        }

        if (!drawnAvatar) {
            ResourceLocation mobTex = MobTextures.byEntityId(currentRecap.killerName());
            if (mobTex != null) {
                MobTextures.blitMobFace(guiGraphics, mobTex, currentRecap.killerName(), avatarX, avatarY, avatarSize);
            } else {
                guiGraphics.fill(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, 0xFF882222);
            }
        }
        guiGraphics.renderOutline(avatarX - 1, avatarY - 1, avatarSize + 2, avatarSize + 2, 0xFF555555);

        int infoX = avatarX + avatarSize + 10;
        int line1Y = cardY + 22;
        guiGraphics.drawString(font, currentRecap.killerName(), infoX, line1Y, 0xFFFFFFFF);

        int line2Y = line1Y + 11;
        float hp = Math.max(0f, currentRecap.killerHealth());
        float maxHp = Math.max(1f, currentRecap.killerMaxHealth());
        String hpStr = String.format(" §c❤ §f%.1f/%.1f", hp, maxHp);
        guiGraphics.drawString(font, hpStr, infoX, line2Y, 0xFFFF7777);

        int line3Y = line2Y + 11;
        StringBuilder tags = new StringBuilder();
        if (currentRecap.isSmash()) tags.append("§6[SMASH] ");
        if (currentRecap.isCritical()) tags.append("§e[CRIT] ");
        if (currentRecap.isLongshot()) tags.append("§b[SNIPER] ");
        if (currentRecap.distance() != null) {
            tags.append(String.format("§7(%.1fm)", currentRecap.distance()));
        }
        if (!tags.isEmpty()) {
            guiGraphics.drawString(font, tags.toString().trim(), infoX, line3Y, 0xFFAAAAAA);
        }

        int weaponX = cardX + cardWidth - 36;
        int weaponY = cardY + 24;
        if (currentRecap.weaponName() != null && !currentRecap.weaponName().equals("minecraft:air")) {
            try {
                ResourceLocation itemRl = ResourceLocation.parse(currentRecap.weaponName());
                ItemStack weaponStack = new ItemStack(BuiltInRegistries.ITEM.get(itemRl));
                guiGraphics.renderItem(weaponStack, weaponX, weaponY);
                guiGraphics.renderItemDecorations(font, weaponStack, weaponX, weaponY);
            } catch (Exception ignored) {}
        }

        int line4Y = cardY + cardHeight - 16;
        Component dmgDealt = Component.translatable("krylix.deathrecap.damage_dealt", String.format("%.1f", currentRecap.damageDealtToKiller()));
        guiGraphics.drawString(font, dmgDealt, cardX + 10, line4Y, 0xFFFFAA00);
    }
}
