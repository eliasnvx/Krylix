package com.example.modid.forge

import com.example.modid.krylix.Krylix
import com.example.modid.krylix.model.KillEntry
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
        val killerName = (source.entity as? LivingEntity)?.name?.string
        val weaponName = getWeaponName(source)
        val distance = calculateDistance(source.entity as? LivingEntity, entity)
        
        val killEntry = KillEntry(
            killerName = killerName,
            victimName = victimName,
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
        val server = ServerLifecycleHooks.getCurrentServer()
        server?.playerList?.players?.forEach { player ->
            // TODO: Отправить уведомление через network packet
            Krylix.LOGGER.debug("Sending kill notification to ${player.name.string}: $killEntry")
        }
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
