package com.eliasnvx.krylix.forge.client

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

/**
 * Клиентский кэш статистики убийств мобов, приходящей с сервера через MobStatsSyncPacket
 * (per-world MobKillStatsData). Используется и HUD-панелью (MobStatsHud), и рейтингом (LeaderboardScreen).
 */
object MobStatsClient {
    private var kills: Map<String, Int> = emptyMap()

    fun updateStats(newKills: Map<String, Int>) {
        kills = newKills
    }

    fun sortedByCount(): List<Map.Entry<String, Int>> = kills.entries.sortedByDescending { it.value }

    fun displayName(entityId: String): String {
        val id = ResourceLocation.tryParse(entityId) ?: return entityId
        val entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null) ?: return entityId
        return Component.translatable(entityType.descriptionId).string
    }
}
