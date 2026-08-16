package com.eliasnvx.krylix.fabric.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class MobTextures {
    private static final Map<String, ResourceLocation> texturesByEntityId = new HashMap<>();
    private static final Map<String, String> displayNameAliases = new HashMap<>();

    private static final ResourceLocation endermanEyes = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/enderman/enderman_eyes.png");
    private static final ResourceLocation breezeEyes = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/breeze/breeze_eyes.png");

    static {
        texturesByEntityId.put("minecraft:zombie", loc("entity/zombie/zombie"));
        texturesByEntityId.put("minecraft:skeleton", loc("entity/skeleton/skeleton"));
        texturesByEntityId.put("minecraft:creeper", loc("entity/creeper/creeper"));
        texturesByEntityId.put("minecraft:spider", loc("entity/spider/spider"));
        texturesByEntityId.put("minecraft:cave_spider", loc("entity/spider/cave_spider"));
        texturesByEntityId.put("minecraft:piglin", loc("entity/piglin/piglin"));
        texturesByEntityId.put("minecraft:piglin_brute", loc("entity/piglin/piglin_brute"));
        texturesByEntityId.put("minecraft:zombified_piglin", loc("entity/piglin/zombified_piglin"));
        texturesByEntityId.put("minecraft:wither_skeleton", loc("entity/skeleton/wither_skeleton"));
        texturesByEntityId.put("minecraft:stray", loc("entity/skeleton/stray"));
        texturesByEntityId.put("minecraft:husk", loc("entity/zombie/husk"));
        texturesByEntityId.put("minecraft:drowned", loc("entity/zombie/drowned"));
        texturesByEntityId.put("minecraft:zombie_villager", loc("entity/zombie_villager/zombie_villager"));
        texturesByEntityId.put("minecraft:blaze", loc("entity/blaze"));
        texturesByEntityId.put("minecraft:ghast", loc("entity/ghast/ghast"));
        texturesByEntityId.put("minecraft:witch", loc("entity/witch"));
        texturesByEntityId.put("minecraft:pillager", loc("entity/illager/pillager"));
        texturesByEntityId.put("minecraft:vindicator", loc("entity/illager/vindicator"));
        texturesByEntityId.put("minecraft:evoker", loc("entity/illager/evoker"));
        texturesByEntityId.put("minecraft:silverfish", loc("entity/silverfish"));
        texturesByEntityId.put("minecraft:endermite", loc("entity/endermite"));
        texturesByEntityId.put("minecraft:iron_golem", loc("entity/iron_golem/iron_golem"));
        texturesByEntityId.put("minecraft:snow_golem", loc("entity/snow_golem"));
        texturesByEntityId.put("minecraft:enderman", loc("entity/enderman/enderman"));
        texturesByEntityId.put("minecraft:ravager", loc("entity/illager/ravager"));
        texturesByEntityId.put("minecraft:warden", loc("entity/warden/warden"));
        texturesByEntityId.put("minecraft:wither", loc("entity/wither/wither"));
        texturesByEntityId.put("minecraft:breeze", loc("entity/breeze/breeze"));
        texturesByEntityId.put("minecraft:bogged", loc("entity/skeleton/bogged"));

        displayNameAliases.put("zombie pigman", "minecraft:zombified_piglin");
        displayNameAliases.put("magmacube", "minecraft:magma_cube");
    }

    private static ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/" + path + ".png");
    }

    public static ResourceLocation byEntityId(String entityId) {
        if (entityId == null) return null;
        return texturesByEntityId.get(entityId.toLowerCase());
    }

    public static String guessEntityId(String mobName) {
        if (mobName == null) return null;
        String normalized = mobName.toLowerCase();
        if (displayNameAliases.containsKey(normalized)) {
            String aliasId = displayNameAliases.get(normalized);
            return aliasId.isEmpty() ? null : aliasId;
        }
        return "minecraft:" + normalized.replace(' ', '_');
    }

    public static ResourceLocation byDisplayName(String mobName) {
        return byEntityId(guessEntityId(mobName));
    }

    public static void blitMobFace(GuiGraphics guiGraphics, ResourceLocation texture, String entityId, int x, int y, int size) {
        String id = entityId != null ? entityId.toLowerCase() : "";
        switch (id) {
            case "minecraft:zombie":
            case "minecraft:husk":
            case "minecraft:drowned":
                guiGraphics.blit(texture, x, y, size, size, 8.0f, 8.0f, 8, 8, 64, 64);
                break;
            case "minecraft:enderman":
                RenderSystem.enableBlend();
                guiGraphics.blit(texture, x, y, size, size, 8.0f, 8.0f, 8, 8, 64, 32);
                guiGraphics.blit(endermanEyes, x, y, size, size, 8.0f, 8.0f, 8, 8, 64, 32);
                RenderSystem.disableBlend();
                break;
            case "minecraft:spider":
            case "minecraft:cave_spider":
                guiGraphics.blit(texture, x, y, size, size, 40.0f, 12.0f, 8, 8, 64, 32);
                break;
            case "minecraft:pillager":
            case "minecraft:vindicator":
            case "minecraft:evoker":
            case "minecraft:zombie_villager":
                guiGraphics.blit(texture, x, y, size, size, 8.0f, 8.0f, 8, 10, 64, 64);
                break;
            case "minecraft:witch":
                guiGraphics.blit(texture, x, y, size, size, 8.0f, 8.0f, 8, 10, 64, 128);
                break;
            case "minecraft:piglin":
            case "minecraft:piglin_brute":
            case "minecraft:zombified_piglin":
                guiGraphics.blit(texture, x, y, size, size, 8.0f, 8.0f, 10, 8, 64, 64);
                break;
            case "minecraft:ghast":
                guiGraphics.blit(texture, x, y, size, size, 16.0f, 16.0f, 16, 16, 64, 32);
                break;
            case "minecraft:iron_golem":
                guiGraphics.blit(texture, x, y, size, size, 8.0f, 8.0f, 8, 10, 128, 128);
                break;
            case "minecraft:snow_golem":
                guiGraphics.blit(texture, x, y, size, size, 8.0f, 8.0f, 8, 8, 64, 64);
                break;
            case "minecraft:ravager":
                guiGraphics.blit(texture, x, y, size, size, 16.0f, 16.0f, 16, 20, 128, 128);
                break;
            case "minecraft:warden":
                guiGraphics.blit(texture, x, y, size, size, 10.0f, 42.0f, 16, 16, 128, 128);
                break;
            case "minecraft:wither":
                guiGraphics.blit(texture, x, y, size, size, 8.0f, 8.0f, 8, 8, 64, 64);
                break;
            case "minecraft:breeze":
                RenderSystem.enableBlend();
                guiGraphics.blit(texture, x, y, size, size, 4.0f, 4.0f, 8, 8, 32, 32);
                guiGraphics.blit(breezeEyes, x, y, size, size, 4.0f, 4.0f, 8, 8, 32, 32);
                RenderSystem.disableBlend();
                break;
            default:
                guiGraphics.blit(texture, x, y, size, size, 8.0f, 8.0f, 8, 8, 64, 32);
                break;
        }
    }
}
