package com.eliasnvx.krylix.core

data class KrylixConfigData(
    /** Сколько секунд запись kill feed остаётся на экране (включая fade-out) */
    var displaySeconds: Int = 15,
    /** Максимум одновременно видимых записей kill feed */
    var maxEntries: Int = 5,
    /** Показывать ли kill feed (тумблер клавишей/командой, сохраняется между сессиями) */
    var killFeedEnabled: Boolean = true,
    /** Показывать ли панель статистики убийств мобов (тумблер клавишей, сохраняется между сессиями) */
    var mobStatsEnabled: Boolean = true,
    /** Максимум строк в панели статистики убийств мобов */
    var mobStatsMaxEntries: Int = 8,
    /** Сервер: рассылать kill feed только игрокам в том же измерении, где произошло убийство */
    var restrictBroadcastToSameDimension: Boolean = false,
    /** Показывать полоску HP над сущностью под прицелом (тумблер клавишей, сохраняется между сессиями) */
    var healthIndicatorEnabled: Boolean = true,
    /** Визуальный стиль полоски HP в индикаторе здоровья */
    var healthBarStyle: HealthBarStyle = HealthBarStyle.BLOCKS,
)
