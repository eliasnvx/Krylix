package com.example.modid.forge

import com.example.modid.krylix.Krylix
import com.example.modid.krylix.model.KillEntry
import com.example.modid.forge.network.NetworkPackets
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraftforge.server.ServerLifecycleHooks

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
        val weaponName = getWeaponName(source)
        val distance = calculateDistance(killerEntity, entity)
        
        val killEntry = KillEntry(
            killerName = killerName,
            killerUUIDString = killerUUIDString,
            victimName = victimName,
            victimUUIDString = victimUUIDString,
            killerHealth = killerHealth,
            weaponName = weaponName,
            distance = distance
        )
        
        activeNotifications.add(killEntry)
        Krylix.LOGGER.info("Kill registered: $killEntry")
        
        // TODO: Отправить уведомление клиентам
        broadcastKillNotification(killEntry)
        
        // Очистка старых уведомлений
        cleanupOldNotifications()
    }
    
    /**
     * Получает название оружия из DamageSource
     */
    private fun getWeaponName(source: DamageSource): String {
        return when {
            source.directEntity is Player -> {
                val player = source.directEntity as Player
                val weapon = player.mainHandItem
                if (weapon.isEmpty) "Fists" else weapon.displayName.string
            }
            source.msgId.contains("arrow") -> "Projectile"
            source.msgId.contains("fire") -> "Fire"
            source.msgId.contains("magic") -> "Magic"
            source.msgId.contains("explosion") -> "Explosion"
            source.msgId.contains("fall") -> "Fall Damage"
            else -> source.msgId
        }
    }
    
    /**
     * Вычисляет дистанцию между атакующим и жертвой
     */
    private fun calculateDistance(attacker: LivingEntity?, victim: LivingEntity): Double? {
        return if (attacker != null && victim != null) {
            attacker.distanceToSqr(victim)
        } else null
    }
    
    /**
     * Отправляет уведомление об убийстве всем игрокам на сервере
     */
    private fun broadcastKillNotification(killEntry: KillEntry) {
        // Отправляем через network packets всем клиентам
        NetworkPackets.sendKillNotificationToAll(killEntry)
        
        Krylix.LOGGER.debug("Broadcasted kill notification to all clients: $killEntry")
    }
    
    /**
     * Очищает устаревшие уведомления
     */
    private fun cleanupOldNotifications() {
        val fadeTime = 5 // 5 секунд
        activeNotifications.removeIf { it.isExpired(fadeTime) }
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
