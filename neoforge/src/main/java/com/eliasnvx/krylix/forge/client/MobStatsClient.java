package com.eliasnvx.krylix.forge.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MobStatsClient {
    private static Map<String, Integer> kills = new HashMap<>();

    public static void updateStats(Map<String, Integer> newKills) {
        kills = newKills;
    }

    public static List<Map.Entry<String, Integer>> sortedByCount() {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(kills.entrySet());
        list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        return list;
    }

    public static String displayName(String entityId) {
        ResourceLocation id = ResourceLocation.tryParse(entityId);
        if (id == null) return entityId;
        Optional<EntityType<?>> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
        if (entityType.isEmpty()) return entityId;
        return Component.translatable(entityType.get().getDescriptionId()).getString();
    }
}
