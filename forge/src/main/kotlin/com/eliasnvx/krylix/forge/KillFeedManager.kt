package com.eliasnvx.krylix.forge

import com.eliasnvx.krylix.Krylix
import com.eliasnvx.krylix.forge.config.KrylixConfig
import com.eliasnvx.krylix.forge.network.NetworkPackets
import com.eliasnvx.krylix.model.KillEntry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player

/**
 * Менеджер для обработки kill feed уведомлений в Forge
 */
object KillFeedManager {
    private val activeNotifications = mutableListOf<KillEntry>()
    var isEnabled = true
        private set

    /**
     * Обрабатывает событие смерти и создает KillEntry
     */
    fun handleDeath(entity: LivingEntity, source: DamageSource) {
        if (!isEnabled || entity.level().isClientSide) return

        val victimName = entity.name.string
        val victimUUIDString = (entity as? Player)?.uuid?.toString()
        val killerEntity = source.entity as? LivingEntity
        val killerName = killerEntity?.name?.string
        val killerUUIDString = (killerEntity as? Player)?.uuid?.toString()
        val killerHealth = (killerEntity as? Player)?.health ?: 20.0f
        val weaponId = getWeaponId(source)
        val distance = calculateDistance(killerEntity, entity)

        val killEntry = KillEntry(
            killerName = killerName,
            killerUUIDString = killerUUIDString,
            victimName = victimName,
            victimUUIDString = victimUUIDString,
            killerHealth = killerHealth,
            weaponName = weaponId,
            distance = distance
        )

        activeNotifications.add(killEntry)
        Krylix.LOGGER.info("Kill registered: $killEntry")

        broadcastKillNotification(killEntry, entity.level() as ServerLevel)

        // Очистка старых уведомлений
        cleanupOldNotifications()
    }

    /**
     * Получает registry id оружия из DamageSource (locale-independent, в отличие от displayName)
     */
    private fun getWeaponId(source: DamageSource): String {
        val directEntity = source.directEntity
        if (directEntity is Player) {
            val weapon = directEntity.mainHandItem
            return if (weapon.isEmpty) "minecraft:air" else BuiltInRegistries.ITEM.getKey(weapon.item).toString()
        }
        return when {
            source.msgId.contains("arrow") -> "minecraft:arrow"
            source.msgId.contains("trident") -> "minecraft:trident"
            source.msgId.contains("fireball") || source.msgId.contains("fire") -> "minecraft:fire_charge"
            source.msgId.contains("magic") -> "minecraft:enchanted_book"
            source.msgId.contains("explosion") -> "minecraft:tnt"
            source.msgId.contains("fall") -> "minecraft:feather"
            else -> "minecraft:air"
        }
    }

    /**
     * Вычисляет дистанцию между атакующим и жертвой
     */
    private fun calculateDistance(attacker: LivingEntity?, victim: LivingEntity): Double? {
        return attacker?.let { kotlin.math.sqrt(it.distanceToSqr(victim)) }
    }

    /**
     * Отправляет уведомление об убийстве игрокам на сервере — всем, либо только тем, кто в том же
     * измерении, где произошло убийство (см. restrictBroadcastToSameDimension в конфиге)
     */
    private fun broadcastKillNotification(killEntry: KillEntry, level: ServerLevel) {
        if (KrylixConfig.get().restrictBroadcastToSameDimension) {
            NetworkPackets.sendKillNotificationToPlayers(level.players(), killEntry)
        } else {
            NetworkPackets.sendKillNotificationToAll(killEntry)
        }

        Krylix.LOGGER.debug("Broadcasted kill notification: $killEntry")
    }

    /**
     * Очищает устаревшие уведомления
     */
    private fun cleanupOldNotifications() {
        activeNotifications.removeIf { it.isExpired(KrylixConfig.get().displaySeconds) }
    }

    /**
     * Включает/выключает kill feed
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        Krylix.LOGGER.info("Kill feed ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Возвращает текущие активные уведомления
     */
    fun getActiveNotifications(): List<KillEntry> {
        return activeNotifications.toList()
    }
}
