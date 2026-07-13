package com.example.modid.krylix.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Представляет запись о убийстве в kill feed (platform-agnostic версия)
 *
 * @property killerName Имя убийцы (может быть null для environmental deaths)
 * @property killerUUIDString UUID убийцы для получения скина (хранится как String)
 * @property victimName Имя жертвы
 * @property victimUUIDString UUID жертвы для получения скина (хранится как String)
 * @property killerHealth HP убийцы после убийства
 * @param weaponName Название оружия/инструмента (строковое представление)
 * @property distance Дистанция убийства в блоках (null если не применимо)
 * @property timestamp Время создания записи в миллисекундах
 * @property isHeadshot Флаг для headshot убийств (для будущего расширения)
 */
@Serializable
data class KillEntry(
    val killerName: String?,
    val killerUUIDString: String? = null,
    val victimName: String,
    val victimUUIDString: String? = null,
    val killerHealth: Float = 20.0f,
    val weaponName: String,
    val distance: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isHeadshot: Boolean = false
) {
    /**
     * Возвращает true если запись устарела и должна быть удалена
     * @param fadeTime время отображения в секундах
     */
    fun isExpired(fadeTime: Int): Boolean {
        val ageInSeconds = (System.currentTimeMillis() - timestamp) / 1000.0
        return ageInSeconds > fadeTime
    }

    /**
     * Возвращает альфа канал для fade out анимации (0.0 - 1.0)
     * @param fadeTime время отображения в секундах
     */
    fun getAlpha(fadeTime: Int): Float {
        val ageInSeconds = (System.currentTimeMillis() - timestamp) / 1000.0
        val fadeStartTime = fadeTime - 1.0 // начинаем fade за 1 секунду до удаления

        return when {
            ageInSeconds < fadeStartTime -> 1.0f
            ageInSeconds >= fadeTime -> 0.0f
            else -> {
                val fadeProgress = (ageInSeconds - fadeStartTime) / 1.0
                (1.0f - fadeProgress.toFloat()).coerceIn(0.0f, 1.0f)
            }
        }
    }

    /**
     * Возвращает true если это environmental death (падение, лава, и т.д.)
     */
    val isEnvironmentalDeath: Boolean
        get() = killerName == null

    /**
     * Возвращает true если это самоубийство
     */
    val isSuicide: Boolean
        get() = killerName == victimName
}
