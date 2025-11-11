package com.example.modid.forge.client

import com.example.modid.krylix.Krylix
import com.example.modid.krylix.model.KillEntry
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.Font
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.network.chat.Style
import net.minecraft.util.Mth
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.resources.ResourceLocation
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.world.entity.player.Player
import com.mojang.authlib.GameProfile
import com.mojang.blaze3d.systems.RenderSystem
import java.awt.Color
import java.util.UUID

/**
 * HUD overlay для отображения kill feed уведомлений
 */
object KillFeedHud {
    private val activeNotifications = mutableListOf<KillEntry>()
    private var isEnabled = true
    
    // Система серий убийств
    private val killStreaks = mutableMapOf<String, KillStreak>()
    
    // Позиция и размеры (горизонтальный layout в одну строку)
    private val maxEntries = 5
    private val entryHeight = 20  // Высота одной строки
    private val headSize = 16     // Размер голов игроков
    private val weaponSize = 16   // Размер оружия
    private val padding = 4       // Отступы между элементами
    private val entrySpacing = 4  // Расстояние между записями по вертикали
    
    // Цвета (без фона)
    private val killerColor = Color(255, 100, 100).rgb
    private val victimColor = Color(100, 150, 255).rgb
    private val hpColor = Color(255, 255, 255).rgb
    
    /**
     * Добавляет новое уведомление в kill feed
     */
    fun addNotification(killEntry: KillEntry) {
        if (!isEnabled) return
        
        activeNotifications.add(killEntry)
        
        // Ограничиваем количество записей
        if (activeNotifications.size > maxEntries) {
            activeNotifications.removeAt(0)
        }
        
        // Обрабатываем серию убийств
        val streakText = processKillStreak(killEntry.killerName)
        if (streakText != null) {
            // Показываем текст серии в чате
            val minecraft = Minecraft.getInstance()
            minecraft.player?.sendSystemMessage(Component.literal("§6§l$streakText"))
            
            // Воспроизводим специальный звук серии
            playStreakSound(streakText)
        } else {
            // Обычный звук килла
            playKillSound()
        }
        
        Krylix.LOGGER.debug("Added kill notification to HUD: $killEntry")
    }
    
    /**
     * Воспроизводит звук при появлении нового килла
     */
    private fun playKillSound() {
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player ?: return
        
        // Используем звук опыта (приятный звук)
        player.level().playSound(
            player,
            player.blockPosition(),
            net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
            net.minecraft.sounds.SoundSource.PLAYERS,
            0.5f, // громкость
            1.2f  // высота тона
        )
    }
    
    /**
     * Парсит название оружия в ItemStack
     */
    private fun parseWeaponItem(weaponName: String?): ItemStack {
        val name = weaponName ?: "Unknown"
        return when {
            name.contains("Sword") -> Items.DIAMOND_SWORD.defaultInstance
            name.contains("Axe") -> Items.IRON_AXE.defaultInstance
            name.contains("Bow") -> Items.BOW.defaultInstance
            name.contains("Crossbow") -> Items.CROSSBOW.defaultInstance
            name.contains("Trident") -> Items.TRIDENT.defaultInstance
            name.contains("Pickaxe") -> Items.IRON_PICKAXE.defaultInstance
            name.contains("Shovel") -> Items.IRON_SHOVEL.defaultInstance
            else -> Items.WOODEN_SWORD.defaultInstance
        }
    }
    
    /**
     * Рендерит kill feed на экране (вертикальный список, каждая запись горизонтально)
     */
    fun render(guiGraphics: GuiGraphics, @Suppress("UNUSED_PARAMETER") partialTick: Float) {
        if (!isEnabled || activeNotifications.isEmpty()) return
        
        val minecraft = Minecraft.getInstance()
        val font = minecraft.font
        val window = minecraft.window
        
        cleanupOldNotifications()
        
        // Позиционирование в правом верхнем углу (учитываем карту)
        val startY = 5
        
        // Создаем копию списка для безопасной итерации (избегаем ConcurrentModificationException)
        val notificationsCopy = activeNotifications.toList()
        
        // Рендерим каждую запись вертикально (списком), каждая запись — горизонтально
        notificationsCopy.forEachIndexed { index, entry ->
            val y = startY + (index * (entryHeight + entrySpacing))
            renderEntry(guiGraphics, font, entry, window.guiScaledWidth, y)
        }
    }
    
    
    /**
     * Рендерит отдельную запись kill feed горизонтально (без фона)
     */
    private fun renderEntry(guiGraphics: GuiGraphics, font: Font, entry: KillEntry, screenWidth: Int, y: Int) {
        val alpha = entry.getAlpha(15)
        
        // Конвертируем UUID String обратно в UUID для рендеринга
        val killerUUID = entry.killerUUIDString?.let { UUID.fromString(it) }
        val victimUUID = entry.victimUUIDString?.let { UUID.fromString(it) }
        
        // Вычисляем ширину записи: голова + ник + оружие + голова + ник + HP текст
        val hpText = "${entry.killerHealth.toInt()}HP"
        val hpWidth = font.width(hpText)
        val killerNameWidth = entry.killerName?.let { font.width(it) } ?: 0
        val victimNameWidth = font.width(entry.victimName)
        val totalWidth = headSize + padding + killerNameWidth + padding + weaponSize + padding + headSize + padding + victimNameWidth + padding + hpWidth
        
        // Позиционируем справа
        val startX = screenWidth - totalWidth - 10
        
        var currentX = startX
        
        // Рендерим голову убийцы
        renderPlayerHead(guiGraphics, killerUUID, entry.killerName, currentX, y, alpha)
        currentX += headSize + padding
        
        // Рендерим ник убийцы
        if (entry.killerName != null) {
            guiGraphics.drawString(font, entry.killerName, currentX, y + 4, getAlphaColor(killerColor, alpha))
            currentX += killerNameWidth + padding
        }
        
        // Рендерим оружие с анимацией (покачивание)
        val weaponItem = parseWeaponItem(entry.weaponName)
        val animationOffset = getWeaponAnimationOffset(entry.timestamp)
        guiGraphics.renderItem(weaponItem, currentX, y + animationOffset)
        currentX += weaponSize + padding
        
        // Рендерим голову жертвы
        renderPlayerHead(guiGraphics, victimUUID, entry.victimName, currentX, y, alpha)
        currentX += headSize + padding
        
        // Рендерим ник жертвы
        guiGraphics.drawString(font, entry.victimName, currentX, y + 4, getAlphaColor(victimColor, alpha))
        currentX += victimNameWidth + padding
        
        // Рендерим HP текст
        guiGraphics.drawString(font, hpText, currentX, y + 4, getAlphaColor(hpColor, alpha))
    }
    
    /**
     * Рендерит голову игрока с реальным скином
     */
    private fun renderPlayerHead(guiGraphics: GuiGraphics, playerUUID: UUID?, playerName: String?, x: Int, y: Int, alpha: Float) {
        val minecraft = Minecraft.getInstance()
        var skinTexture: ResourceLocation? = null
        var isMob = false
        
        // Сначала пробуем найти игрока по UUID в текущей сессии
        if (playerUUID != null) {
            val connection = minecraft.connection
            val playerInfo = connection?.getPlayerInfo(playerUUID)
            if (playerInfo != null) {
                skinTexture = playerInfo.skinLocation
            }
        }
        
        // Если не нашли по UUID, пробуем найти по никнейму
        if (skinTexture == null && playerName != null) {
            val connection = minecraft.connection
            // Ищем игрока по никнейму в списке онлайн игроков
            val playerInfo = connection?.onlinePlayers?.firstOrNull { 
                it.profile.name.equals(playerName, ignoreCase = true) 
            }
            if (playerInfo != null) {
                skinTexture = playerInfo.skinLocation
            } else {
                // Проверяем, не является ли это мобом
                skinTexture = getMobTexture(playerName)
                if (skinTexture != null) {
                    isMob = true
                } else {
                    // Если игрока нет онлайн, создаём GameProfile и загружаем скин
                    try {
                        val gameProfile = GameProfile(playerUUID, playerName)
                        val skinManager = minecraft.skinManager
                        skinTexture = skinManager.getInsecureSkinLocation(gameProfile)
                    } catch (e: Exception) {
                        Krylix.LOGGER.debug("Failed to load skin for $playerName: ${e.message}")
                    }
                }
            }
        }
        
        // Рендерим голову со скином или fallback
        if (skinTexture != null) {
            try {
                RenderSystem.setShaderTexture(0, skinTexture)
                RenderSystem.enableBlend()
                
                if (isMob) {
                    // Для мобов рендерим лицо с другими UV координатами
                    renderMobFace(guiGraphics, skinTexture, playerName, x, y)
                } else {
                    // Для игроков рендерим базовый слой головы (8, 8)
                    guiGraphics.blit(skinTexture, x, y, headSize, headSize, 8.0f, 8.0f, 8, 8, 64, 64)
                    // Рендерим overlay слой головы (40, 8)
                    guiGraphics.blit(skinTexture, x, y, headSize, headSize, 40.0f, 8.0f, 8, 8, 64, 64)
                }
                
                RenderSystem.disableBlend()
            } catch (e: Exception) {
                Krylix.LOGGER.debug("Failed to render skin texture for $playerName: ${e.message}")
                renderFallbackHead(guiGraphics, playerName, x, y, alpha)
            }
        } else {
            renderFallbackHead(guiGraphics, playerName, x, y, alpha)
        }
    }
    
    /**
     * Получает текстуру моба по его имени
     */
    private fun getMobTexture(mobName: String?): ResourceLocation? {
        if (mobName == null) return null
        
        return when (mobName.lowercase()) {
            "zombie" -> ResourceLocation("minecraft", "textures/entity/zombie/zombie.png")
            "skeleton" -> ResourceLocation("minecraft", "textures/entity/skeleton/skeleton.png")
            "creeper" -> ResourceLocation("minecraft", "textures/entity/creeper/creeper.png")
            "spider" -> ResourceLocation("minecraft", "textures/entity/spider/spider.png")
            "cave spider" -> ResourceLocation("minecraft", "textures/entity/spider/cave_spider.png")
            "enderman" -> ResourceLocation("minecraft", "textures/entity/enderman/enderman.png")
            "piglin" -> ResourceLocation("minecraft", "textures/entity/piglin/piglin.png")
            "piglin brute" -> ResourceLocation("minecraft", "textures/entity/piglin/piglin_brute.png")
            "zombified piglin", "zombie pigman" -> ResourceLocation("minecraft", "textures/entity/piglin/zombified_piglin.png")
            "wither skeleton" -> ResourceLocation("minecraft", "textures/entity/skeleton/wither_skeleton.png")
            "stray" -> ResourceLocation("minecraft", "textures/entity/skeleton/stray.png")
            "husk" -> ResourceLocation("minecraft", "textures/entity/zombie/husk.png")
            "drowned" -> ResourceLocation("minecraft", "textures/entity/zombie/drowned.png")
            "blaze" -> ResourceLocation("minecraft", "textures/entity/blaze.png")
            "ghast" -> ResourceLocation("minecraft", "textures/entity/ghast/ghast.png")
            "slime" -> ResourceLocation("minecraft", "textures/entity/slime/slime.png")
            "magma cube", "magmacube" -> ResourceLocation("minecraft", "textures/entity/slime/magmacube.png")
            "witch" -> ResourceLocation("minecraft", "textures/entity/witch.png")
            "pillager" -> ResourceLocation("minecraft", "textures/entity/illager/pillager.png")
            "vindicator" -> ResourceLocation("minecraft", "textures/entity/illager/vindicator.png")
            "evoker" -> ResourceLocation("minecraft", "textures/entity/illager/evoker.png")
            "ravager" -> ResourceLocation("minecraft", "textures/entity/illager/ravager.png")
            "vex" -> ResourceLocation("minecraft", "textures/entity/illager/vex.png")
            "phantom" -> ResourceLocation("minecraft", "textures/entity/phantom.png")
            "shulker" -> ResourceLocation("minecraft", "textures/entity/shulker/shulker.png")
            "silverfish" -> ResourceLocation("minecraft", "textures/entity/silverfish.png")
            "endermite" -> ResourceLocation("minecraft", "textures/entity/endermite.png")
            "guardian" -> ResourceLocation("minecraft", "textures/entity/guardian.png")
            "elder guardian" -> ResourceLocation("minecraft", "textures/entity/guardian_elder.png")
            "wither" -> ResourceLocation("minecraft", "textures/entity/wither/wither.png")
            "ender dragon" -> ResourceLocation("minecraft", "textures/entity/enderdragon/dragon.png")
            else -> null
        }
    }
    
    /**
     * Рендерит лицо моба (упрощённо — часть текстуры)
     */
    private fun renderMobFace(guiGraphics: GuiGraphics, texture: ResourceLocation, mobName: String?, x: Int, y: Int) {
        // Для разных мобов разные UV координаты
        when (mobName?.lowercase()) {
            "zombie", "husk", "drowned" -> {
                // Для зомби текстура 64x64, лицо в (8, 8)
                guiGraphics.blit(texture, x, y, headSize, headSize, 8.0f, 8.0f, 8, 8, 64, 64)
            }
            "enderman" -> {
                // Для эндермена текстура 64x32, голова 8x8 в позиции (0, 0)
                guiGraphics.blit(texture, x, y, headSize, headSize, 0.0f, 0.0f, 8, 8, 64, 32)
            }
            else -> {
                // Для остальных мобов текстуры 64x32, лицо в (8, 8)
                guiGraphics.blit(texture, x, y, headSize, headSize, 8.0f, 8.0f, 8, 8, 64, 32)
            }
        }
    }
    
    /**
     * Рендерит запасную голову (цветной квадратик)
     */
    private fun renderFallbackHead(guiGraphics: GuiGraphics, playerName: String?, x: Int, y: Int, alpha: Float) {
        val name = playerName ?: "Unknown"
        val color = if (name.contains("1", ignoreCase = true)) killerColor else victimColor
        guiGraphics.fill(x, y, x + headSize, y + headSize, getAlphaColor(color, alpha))
    }
    
    
    /**
     * Конвертирует цвет с альфа-каналом
     */
    private fun getAlphaColor(baseColor: Int, alpha: Float): Int {
        val alphaInt = (alpha * 255).toInt()
        return (baseColor and 0xFFFFFF) or (alphaInt shl 24)
    }
    
    /**
     * Вычисляет смещение для анимации оружия (покачивание вверх-вниз)
     */
    private fun getWeaponAnimationOffset(timestamp: Long): Int {
        val currentTime = System.currentTimeMillis()
        val age = currentTime - timestamp
        
        // Синусоидальное покачивание с периодом 1 секунда
        val frequency = 2.0 // Герцы (2 колебания в секунду)
        val amplitude = 2.0 // Амплитуда в пикселях
        
        val offset = amplitude * kotlin.math.sin(age / 1000.0 * frequency * 2.0 * kotlin.math.PI)
        return offset.toInt()
    }
    
    /**
     * Очищает устаревшие уведомления
     */
    private fun cleanupOldNotifications() {
        activeNotifications.removeIf { it.isExpired(15) }
    }
    
    /**
     * Включает/выключает HUD
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (!enabled) {
            activeNotifications.clear()
        }
        Krylix.LOGGER.info("Kill feed HUD ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Включен ли HUD
     */
    fun isHudEnabled(): Boolean = isEnabled
    
    /**
     * Возвращает количество активных уведомлений
     */
    fun getNotificationCount(): Int = activeNotifications.size
    
    /**
     * Добавляет тестовое уведомление для проверки рендеринга
     */
    fun addTestNotification() {
        val testEntry = KillEntry(
            killerName = "TestKiller",
            killerUUIDString = "12345678-1234-1234-1234-123456789abc",
            victimName = "TestVictim", 
            victimUUIDString = "87654321-4321-4321-4321-cba987654321",
            killerHealth = 18.0f,
            weaponName = "Diamond Sword",
            distance = 5.0
        )
        activeNotifications.add(testEntry)
        Krylix.LOGGER.info("Added test kill notification for debugging")
    }
    
    /**
     * Очищает все уведомления
     */
    fun clearNotifications() {
        activeNotifications.clear()
        killStreaks.clear()
        Krylix.LOGGER.info("Cleared all kill notifications")
    }
    
    /**
     * Обрабатывает серию убийств для киллера
     */
    private fun processKillStreak(killerName: String?): String? {
        if (killerName == null) return null
        
        val currentTime = System.currentTimeMillis()
        val streak = killStreaks.getOrPut(killerName) { KillStreak(killerName, 0, currentTime) }
        
        // Если прошло больше 10 секунд с последнего килла - сбрасываем серию
        if (currentTime - streak.lastKillTime > 10000) {
            streak.count = 1
        } else {
            streak.count++
        }
        
        streak.lastKillTime = currentTime
        
        // Возвращаем текст серии
        return when (streak.count) {
            2 -> "DOUBLE KILL!"
            3 -> "TRIPLE KILL!"
            4 -> "MEGA KILL!"
            5 -> "ULTRA KILL!"
            6 -> "MONSTER KILL!"
            7 -> "RAMPAGE!"
            else -> if (streak.count > 7) "GODLIKE!" else null
        }
    }
    
    /**
     * Воспроизводит звук серии убийств
     */
    private fun playStreakSound(streakText: String?) {
        if (streakText == null) return
        
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player ?: return
        
        // Разные звуки для разных серий
        val (sound, volume, pitch) = when (streakText) {
            "DOUBLE KILL!" -> Triple(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 0.7f, 1.2f)
            "TRIPLE KILL!" -> Triple(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 0.8f, 1.4f)
            "MEGA KILL!" -> Triple(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 0.9f, 1.6f)
            "ULTRA KILL!" -> Triple(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 1.0f, 1.8f)
            else -> Triple(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 1.0f, 2.0f)
        }
        
        player.level().playSound(
            player,
            player.blockPosition(),
            sound,
            net.minecraft.sounds.SoundSource.PLAYERS,
            volume,
            pitch
        )
    }
}

/**
 * Класс для отслеживания серии убийств игрока
 */
private data class KillStreak(
    val playerName: String,
    var count: Int,
    var lastKillTime: Long
)
