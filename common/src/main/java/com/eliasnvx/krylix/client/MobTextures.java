package com.eliasnvx.krylix.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public class MobTextures {
    private static final Map<String, Identifier> texturesByEntityId = new HashMap<>();

    private static final Identifier endermanEyes = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/enderman/enderman_eyes.png");
    private static final Identifier breezeEyes = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/breeze/breeze_eyes.png");

    static {
        register("minecraft:zombie", "zombie/zombie");
        register("minecraft:skeleton", "skeleton/skeleton");
        register("minecraft:creeper", "creeper/creeper");
        register("minecraft:spider", "spider/spider");
        register("minecraft:cave_spider", "spider/cave_spider");
        register("minecraft:enderman", "enderman/enderman");
        register("minecraft:witch", "witch/witch");
        register("minecraft:slime", "slime/slime");
        register("minecraft:magma_cube", "slime/magmacube");
        register("minecraft:blaze", "blaze/blaze");
        register("minecraft:ghast", "ghast/ghast");
        register("minecraft:wither_skeleton", "skeleton/wither_skeleton");
        register("minecraft:drowned", "zombie/drowned");
        register("minecraft:husk", "zombie/husk");
        register("minecraft:zombie_villager", "zombie_villager/zombie_villager");
        register("minecraft:stray", "skeleton/stray");
        register("minecraft:phantom", "phantom/phantom");
        register("minecraft:pillager", "illager/pillager");
        register("minecraft:vindicator", "illager/vindicator");
        register("minecraft:evoker", "illager/evoker");
        register("minecraft:ravager", "illager/ravager");
        register("minecraft:vex", "illager/vex");
        register("minecraft:guardian", "guardian/guardian");
        register("minecraft:elder_guardian", "guardian/guardian_elder");
        register("minecraft:shulker", "shulker/shulker");
        register("minecraft:piglin", "piglin/piglin");
        register("minecraft:piglin_brute", "piglin/piglin_brute");
        register("minecraft:zombified_piglin", "piglin/zombified_piglin");
        register("minecraft:hoglin", "hoglin/hoglin");
        register("minecraft:zoglin", "hoglin/zoglin");
        register("minecraft:warden", "warden/warden");
        register("minecraft:breeze", "breeze/breeze");
        register("minecraft:bogged", "skeleton/bogged");
        register("minecraft:wither", "wither/wither");
        register("minecraft:ender_dragon", "enderdragon/dragon");
        register("minecraft:iron_golem", "iron_golem/iron_golem");
        register("minecraft:snow_golem", "snow_golem/snow_golem");
    }

    private static void register(String entityId, String path) {
        texturesByEntityId.put(entityId, loc(path));
    }

    private static Identifier loc(String path) {
        return Identifier.fromNamespaceAndPath("minecraft", "textures/entity/" + path + ".png");
    }

    public static Identifier byEntityId(String entityId) {
        return texturesByEntityId.get(entityId);
    }

    public static void blitMobFace(GuiGraphicsExtractor guiGraphics, Identifier texture, String entityId, int x, int y, int size) {
        String id = entityId != null ? entityId.toLowerCase() : "";
        switch (id) {
            case "minecraft:skeleton":
            case "minecraft:creeper":
            case "minecraft:slime":
            case "minecraft:blaze":
            case "minecraft:wither_skeleton":
            case "minecraft:stray":
            case "minecraft:bogged":
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 8.0f, 8.0f, size, size, 8, 8, 64, 32);
                break;
            case "minecraft:enderman":
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 8.0f, 8.0f, size, size, 8, 8, 64, 32);
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, endermanEyes, x, y, 8.0f, 8.0f, size, size, 8, 8, 64, 32);
                break;
            case "minecraft:spider":
            case "minecraft:cave_spider":
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 40.0f, 12.0f, size, size, 8, 8, 64, 32);
                break;
            case "minecraft:witch":
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 8.0f, 8.0f, size, size, 8, 10, 64, 128);
                break;
            case "minecraft:pillager":
            case "minecraft:vindicator":
            case "minecraft:evoker":
            case "minecraft:zombie_villager":
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 8.0f, 8.0f, size, size, 8, 10, 64, 64);
                break;
            case "minecraft:ghast":
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 16.0f, 16.0f, size, size, 16, 16, 128, 64);
                break;
            case "minecraft:iron_golem":
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 8.0f, 8.0f, size, size, 8, 10, 128, 128);
                break;
            case "minecraft:ravager":
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 16.0f, 16.0f, size, size, 16, 20, 128, 128);
                break;
            case "minecraft:warden":
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 10.0f, 42.0f, size, size, 16, 16, 128, 128);
                break;
            case "minecraft:vex":
            case "minecraft:breeze":
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 4.0f, 4.0f, size, size, 8, 8, 32, 32);
                if ("minecraft:breeze".equals(id)) {
                    guiGraphics.blit(RenderPipelines.GUI_TEXTURED, breezeEyes, x, y, 4.0f, 4.0f, size, size, 8, 8, 32, 32);
                }
                break;
            case "minecraft:hoglin":
            case "minecraft:zoglin":
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 80.0f, 5.0f, size, size, 14, 14, 128, 64);
                break;
            case "minecraft:zombie":
            case "minecraft:husk":
            case "minecraft:drowned":
            case "minecraft:piglin":
            case "minecraft:piglin_brute":
            case "minecraft:zombified_piglin":
            case "minecraft:wither":
            case "minecraft:snow_golem":
            case "minecraft:phantom":
            case "minecraft:guardian":
            case "minecraft:elder_guardian":
            case "minecraft:shulker":
            case "minecraft:magma_cube":
            default:
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 8.0f, 8.0f, size, size, 8, 8, 64, 64);
                break;
        }
    }
}
