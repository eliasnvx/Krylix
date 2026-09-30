package com.eliasnvx.krylix.client;

import com.eliasnvx.krylix.api.client.MobHead;
import com.eliasnvx.krylix.network.KrylixPayloads.Combatant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Heads and item icons shared by the feed, the recap, the leaderboard and the mob panel. */
public final class Avatars {
    private static final int FALLBACK_COLOR = 0xFF55555F;
    private static final Map<String, ItemStack> ITEMS = new HashMap<>();

    private Avatars() {
    }

    public static void draw(GuiGraphicsExtractor graphics, Combatant who, int x, int y, int size) {
        draw(graphics, who.uuid(), who.entityType(), x, y, size);
    }

    /** A player's face (skin with hat layer) when {@code player} is set, otherwise the mob's face. */
    public static void draw(GuiGraphicsExtractor graphics, @Nullable UUID player, @Nullable String entityType, int x, int y, int size) {
        draw(graphics, player, entityType, x, y, size, -1);
    }

    /** @param color tint as ARGB; {@code -1} draws it as is, a lower alpha fades it (the kill feed rows) */
    public static void draw(GuiGraphicsExtractor graphics, @Nullable UUID player, @Nullable String entityType, int x, int y, int size, int color) {
        if (player != null) {
            PlayerFaceExtractor.extractRenderState(graphics, skinOf(player), x, y, size, true, false, color);
            return;
        }
        MobHead head = entityType != null ? MobHeads.get(entityType) : null;
        if (head != null) {
            MobHeads.draw(graphics, head, x, y, size, color);
        } else {
            int alpha = (color >>> 24) * (FALLBACK_COLOR >>> 24) / 255;
            HudRender.roundedFill(graphics, x, y, size, size, Math.max(1, size / 6), (alpha << 24) | (FALLBACK_COLOR & 0xFFFFFF));
        }
    }

    /** Left a server: its item ids don't matter for the next one. */
    public static void clearCache() {
        ITEMS.clear();
    }

    /** The player's skin if they are on this server, else the default skin their UUID gets. */
    public static Identifier skinOf(UUID uuid) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            PlayerInfo info = connection.getPlayerInfo(uuid);
            if (info != null) {
                return info.getSkin().body().texturePath();
            }
        }
        return DefaultPlayerSkin.get(uuid).body().texturePath();
    }

    /** The item for an id, or empty for air / unknown ids. Cached: rows ask for it every frame. */
    public static ItemStack item(String id) {
        return ITEMS.computeIfAbsent(id, key -> {
            Identifier parsed = Identifier.tryParse(key);
            if (parsed == null) {
                return ItemStack.EMPTY;
            }
            Item item = BuiltInRegistries.ITEM.getValue(parsed);
            return item == Items.AIR ? ItemStack.EMPTY : item.getDefaultInstance();
        });
    }
}
