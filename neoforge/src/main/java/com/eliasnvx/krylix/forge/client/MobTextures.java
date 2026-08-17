package com.eliasnvx.krylix.forge.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public class MobTextures {
    private static final Map<String, Identifier> texturesByEntityId = new HashMap<>();
    private static final Map<String, String> entityIdByDisplayName = new HashMap<>();

    private static final Identifier endermanEyes = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/enderman/enderman_eyes.png");
    private static final Identifier breezeEyes = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/breeze/breeze_eyes.png");

    static {
        register("minecraft:zombie", "zombie/zombie", "Zombie");
        register("minecraft:skeleton", "skeleton/skeleton", "Skeleton");
        register("minecraft:creeper", "creeper/creeper", "Creeper");
        register("minecraft:spider", "spider/spider", "Spider");
        register("minecraft:cave_spider", "spider/cave_spider", "Cave Spider");
        register("minecraft:enderman", "enderman/enderman", "Enderman");
        register("minecraft:witch", "witch/witch", "Witch");
        register("minecraft:slime", "slime/slime", "Slime");
        register("minecraft:magma_cube", "slime/magmacube", "Magma Cube");
        register("minecraft:blaze", "blaze", "Blaze");
        register("minecraft:ghast", "ghast/ghast", "Ghast");
        register("minecraft:wither_skeleton", "skeleton/wither_skeleton", "Wither Skeleton");
        register("minecraft:drowned", "zombie/drowned", "Drowned");
        register("minecraft:husk", "zombie/husk", "Husk");
        register("minecraft:stray", "skeleton/stray", "Stray");
        register("minecraft:phantom", "phantom", "Phantom");
        register("minecraft:pillager", "illager/pillager", "Pillager");
        register("minecraft:vindicator", "illager/vindicator", "Vindicator");
        register("minecraft:evoker", "illager/evoker", "Evoker");
        register("minecraft:ravager", "illager/ravager", "Ravager");
        register("minecraft:vex", "illager/vex", "Vex");
        register("minecraft:guardian", "guardian", "Guardian");
        register("minecraft:elder_guardian", "guardian_elder", "Elder Guardian");
        register("minecraft:shulker", "shulker/shulker_ender", "Shulker");
        register("minecraft:piglin", "piglin/piglin", "Piglin");
        register("minecraft:piglin_brute", "piglin/piglin_brute", "Piglin Brute");
        register("minecraft:zombified_piglin", "piglin/zombified_piglin", "Zombified Piglin");
        register("minecraft:hoglin", "hoglin/hoglin", "Hoglin");
        register("minecraft:zoglin", "hoglin/zoglin", "Zoglin");
        register("minecraft:warden", "warden/warden", "Warden");
        register("minecraft:breeze", "breeze/breeze", "Breeze");
        register("minecraft:bogged", "skeleton/bogged", "Bogged");
        register("minecraft:wither", "wither/wither", "Wither");
        register("minecraft:ender_dragon", "enderdragon/dragon", "Ender Dragon");
        register("minecraft:iron_golem", "iron_golem/iron_golem", "Iron Golem");
        register("minecraft:snow_golem", "snow_golem", "Snow Golem");
    }

    private static void register(String entityId, String path, String displayName) {
        texturesByEntityId.put(entityId, loc(path));
        entityIdByDisplayName.put(displayName.toLowerCase(), entityId);
    }

    private static Identifier loc(String path) {
        return Identifier.fromNamespaceAndPath("minecraft", "textures/" + path + ".png");
    }

    public static Identifier byEntityId(String entityId) {
        return texturesByEntityId.get(entityId);
    }

    public static String guessEntityId(String displayName) {
        if (displayName == null) return null;
        String lower = displayName.toLowerCase().trim();
        String id = entityIdByDisplayName.get(lower);
        if (id != null) return id;
        for (Map.Entry<String, String> entry : entityIdByDisplayName.entrySet()) {
            if (lower.contains(entry.getKey())) return entry.getValue();
        }
        return null;
    }

    public static Identifier byDisplayName(String mobName) {
        return byEntityId(guessEntityId(mobName));
    }

    public static void blitMobFace(GuiGraphicsExtractor guiGraphics, Identifier texture, String entityId, int x, int y, int size) {
        String id = entityId != null ? entityId.toLowerCase() : "";
        switch (id) {
            case "minecraft:zombie":
            case "minecraft:husk":
            case "minecraft:drowned":
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 8.0f, 8.0f, 8, 8, 64, 64, size, size);
                break;
            case "minecraft:enderman":
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 8.0f, 8.0f, 8, 8, 64, 32, size, size);
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, endermanEyes, x, y, 8.0f, 8.0f, 8, 8, 64, 32, size, size);
                break;
            case "minecraft:spider":
            case "minecraft:cave_spider":
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 40.0f, 12.0f, 8, 8, 64, 32, size, size);
                break;
            case "minecraft:pillager":
            case "minecraft:vindicator":
            case "minecraft:evoker":
            case "minecraft:zombie_villager":
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 8.0f, 8.0f, 8, 10, 64, 64, size, size);
                break;
            case "minecraft:witch":
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 8.0f, 8.0f, 8, 10, 64, 128, size, size);
                break;
            case "minecraft:piglin":
            case "minecraft:piglin_brute":
            case "minecraft:zombified_piglin":
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 8.0f, 8.0f, 8, 8, 64, 64, size, size);
                break;
            case "minecraft:ghast":
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 16.0f, 16.0f, 16, 16, 64, 32, size, size);
                break;
            case "minecraft:iron_golem":
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 8.0f, 8.0f, 8, 10, 128, 128, size, size);
                break;
            case "minecraft:snow_golem":
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 8.0f, 8.0f, 8, 8, 64, 64, size, size);
                break;
            case "minecraft:ravager":
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 16.0f, 16.0f, 16, 20, 128, 128, size, size);
                break;
            case "minecraft:warden":
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 10.0f, 42.0f, 16, 16, 128, 128, size, size);
                break;
            case "minecraft:wither":
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 8.0f, 8.0f, 8, 8, 64, 64, size, size);
                break;
            case "minecraft:breeze":
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 4.0f, 4.0f, 8, 8, 32, 32, size, size);
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, breezeEyes, x, y, 4.0f, 4.0f, 8, 8, 32, 32, size, size);
                break;
            default:
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 8.0f, 8.0f, 8, 8, 64, 32, size, size);
                break;
        }
    }
}
