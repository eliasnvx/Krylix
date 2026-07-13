package com.eliasnvx.krylix.forge.client

import com.eliasnvx.krylix.Krylix
import com.eliasnvx.krylix.model.KillEntry
import com.mojang.authlib.GameProfile
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.awt.Color
import java.util.UUID

/**
 * HUD overlay для отображения kill feed уведомлений
 */
object KillFeedHud {
    private val activeNotifications = mutableListOf<KillEntry>()
    private val isEnabled get() = com.eliasnvx.krylix.forge.config.KrylixConfig.get().killFeedEnabled

    // Кэш скинов для оффлайн/неизвестных игроков (SkinManager иначе резолвился бы каждый кадр)
    private val offlineSkinCache = mutableMapOf<String, ResourceLocation?>()

    // Позиция и размеры (горизонтальный layout в одну строку)
    private val maxEntries get() = com.eliasnvx.krylix.forge.config.KrylixConfig.get().maxEntries
    private val displaySeconds get() = com.eliasnvx.krylix.forge.config.KrylixConfig.get().displaySeconds
    private val entryHeight = 16 // Высота одной строки
    private val headSize = 12 // Размер голов игроков
    private val weaponSize = 12 // Размер оружия
    private val padding = 4 // Отступы между элементами
    private val entrySpacing = 4 // Расстояние между записями по вертикали
    private val avatarCornerCut = 2 // Срез уголков аватарки в px (лёгкое скругление)
    private val heartSize = 9 // Ванильная иконка сердца в icons.png — 9x9

    // Цвета (без фона)
    private val killerColor = Color(255, 100, 100).rgb
    private val victimColor = Color(100, 150, 255).rgb
    private val hpColor = Color(255, 255, 255).rgb

    private val iconsTexture = ResourceLocation("minecraft", "textures/gui/icons.png")

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

        playKillSound()

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
            1.2f // высота тона
        )
    }

    /**
     * Резолвит weaponId (registry id, напр. "minecraft:diamond_sword") в ItemStack.
     * Матчинг по id вместо displayName — устойчив к локализации и переименованным/модовым предметам.
     */
    private fun parseWeaponItem(weaponId: String?): ItemStack {
        val id = weaponId?.let { ResourceLocation.tryParse(it) } ?: return Items.IRON_SWORD.defaultInstance
        val item = BuiltInRegistries.ITEM.get(id)
        return if (item == Items.AIR) Items.IRON_SWORD.defaultInstance else item.defaultInstance
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
        val alpha = entry.getAlpha(displaySeconds)

        // Нет осмысленного "убийцы" — окружение или сам себя. Показываем упрощённую запись
        // без пустой головы убийцы, а не двойной/пустой аватар.
        if (entry.killerName == null || entry.isSuicide) {
            renderSelfKillEntry(guiGraphics, font, entry, screenWidth, y, alpha)
            return
        }

        // Конвертируем UUID String обратно в UUID для рендеринга
        val killerUUID = entry.killerUUIDString?.let { UUID.fromString(it) }
        val victimUUID = entry.victimUUIDString?.let { UUID.fromString(it) }

        // Вычисляем ширину записи: голова + ник + оружие + голова + ник + HP (число + сердечко)
        val hpText = "${entry.killerHealth.toInt()}"
        val hpWidth = font.width(hpText) + 2 + heartSize
        val killerNameWidth = font.width(entry.killerName)
        val victimNameWidth = font.width(entry.victimName)
        val totalWidth = headSize + padding + killerNameWidth + padding + weaponSize + padding + headSize + padding + victimNameWidth + padding + hpWidth

        // Позиционируем справа
        val startX = screenWidth - totalWidth - 10

        var currentX = startX

        // Рендерим голову убийцы
        renderPlayerHead(guiGraphics, killerUUID, entry.killerName, currentX, y, alpha)
        currentX += headSize + padding

        // Рендерим ник убийцы
        guiGraphics.drawString(font, entry.killerName, currentX, y + 4, getAlphaColor(killerColor, alpha))
        currentX += killerNameWidth + padding

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

        // Рендерим HP: число + иконка сердца вместо текста "HP"
        guiGraphics.drawString(font, hpText, currentX, y + 4, getAlphaColor(hpColor, alpha))
        renderHeartIcon(guiGraphics, currentX + font.width(hpText) + 2, y + 1, alpha)
    }

    /**
     * Рендерит ванильную иконку красного сердца (icons.png, 52x0, 9x9) вместо текста "HP"
     */
    private fun renderHeartIcon(guiGraphics: GuiGraphics, x: Int, y: Int, alpha: Float) {
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha)
        guiGraphics.blit(iconsTexture, x, y, heartSize, heartSize, 52.0f, 0.0f, heartSize, heartSize, 256, 256)
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
    }

    /**
     * Упрощённая запись для environmental death / suicide: голова жертвы + иконка причины смерти,
     * без несуществующего "убийцы" и без лишней подписи.
     */
    private fun renderSelfKillEntry(guiGraphics: GuiGraphics, font: Font, entry: KillEntry, screenWidth: Int, y: Int, alpha: Float) {
        val victimUUID = entry.victimUUIDString?.let { UUID.fromString(it) }
        val victimNameWidth = font.width(entry.victimName)
        val totalWidth = headSize + padding + victimNameWidth + padding + weaponSize
        val startX = screenWidth - totalWidth - 10

        var currentX = startX

        renderPlayerHead(guiGraphics, victimUUID, entry.victimName, currentX, y, alpha)
        currentX += headSize + padding

        guiGraphics.drawString(font, entry.victimName, currentX, y + 4, getAlphaColor(victimColor, alpha))
        currentX += victimNameWidth + padding

        val weaponItem = parseWeaponItem(entry.weaponName)
        val animationOffset = getWeaponAnimationOffset(entry.timestamp)
        guiGraphics.renderItem(weaponItem, currentX, y + animationOffset)
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
                skinTexture = MobTextures.byDisplayName(playerName)
                if (skinTexture != null) {
                    isMob = true
                } else if (playerUUID != null) {
                    // UUID есть только у настоящих игроков (см. KillFeedManager) — значит это
                    // оффлайн-игрок, а не неизвестный моб. Резолвим скин через SkinManager и кэшируем.
                    // Без этого условия неизвестные мобы (напр. голем) получали случайный
                    // дефолтный скин Стива/Алекса от SkinManager вместо честного fallback-квадрата.
                    skinTexture = offlineSkinCache.getOrPut(playerUUID.toString()) {
                        try {
                            minecraft.skinManager.getInsecureSkinLocation(GameProfile(playerUUID, playerName))
                        } catch (e: Exception) {
                            Krylix.LOGGER.debug("Failed to load skin for $playerName: ${e.message}")
                            null
                        }
                    }
                }
            }
        }

        // Рендерим голову со скином или fallback
        if (skinTexture != null) {
            try {
                RenderSystem.setShaderTexture(0, skinTexture)
                RenderSystem.enableBlend()

                HudRender.rounded(guiGraphics, x, y, headSize, avatarCornerCut) {
                    if (isMob) {
                        // Для мобов рендерим лицо с другими UV координатами
                        MobTextures.blitMobFace(guiGraphics, skinTexture, MobTextures.guessEntityId(playerName), x, y, headSize)
                    } else {
                        // Для игроков рендерим базовый слой головы (8, 8)
                        guiGraphics.blit(skinTexture, x, y, headSize, headSize, 8.0f, 8.0f, 8, 8, 64, 64)
                        // Рендерим overlay слой головы (40, 8)
                        guiGraphics.blit(skinTexture, x, y, headSize, headSize, 40.0f, 8.0f, 8, 8, 64, 64)
                    }
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
     * Рендерит запасную голову (цветной квадратик)
     */
    private fun renderFallbackHead(guiGraphics: GuiGraphics, playerName: String?, x: Int, y: Int, alpha: Float) {
        val name = playerName ?: "Unknown"
        val color = if (name.contains("1", ignoreCase = true)) killerColor else victimColor
        HudRender.rounded(guiGraphics, x, y, headSize, avatarCornerCut) {
            guiGraphics.fill(x, y, x + headSize, y + headSize, getAlphaColor(color, alpha))
        }
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
        activeNotifications.removeIf { it.isExpired(displaySeconds) }
    }

    /**
     * Включает/выключает HUD. Сохраняется в конфиг — не сбрасывается при перезапуске.
     */
    fun setEnabled(enabled: Boolean) {
        val config = com.eliasnvx.krylix.forge.config.KrylixConfig.get()
        config.killFeedEnabled = enabled
        com.eliasnvx.krylix.forge.config.KrylixConfig.save()
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
     * Очищает все уведомления
     */
    fun clearNotifications() {
        activeNotifications.clear()
        offlineSkinCache.clear()
        Krylix.LOGGER.info("Cleared all kill notifications")
    }
}
