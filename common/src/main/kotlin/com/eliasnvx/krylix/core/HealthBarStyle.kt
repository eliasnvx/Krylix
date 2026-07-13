package com.eliasnvx.krylix.core

/**
 * Визуальный стиль полоски HP в индикаторе здоровья под прицелом (см. HealthIndicator).
 *
 * toString() переопределён на ключ перевода — Cloth Config берёт enum.toString() как
 * translation key для выпадающего списка (см. EnumListEntry), а голое имя константы
 * без этого не переводилось бы ни на один язык.
 */
enum class HealthBarStyle {
    BLOCKS,
    ASCII,
    DOTS,
    NUMBER_ONLY,
    ;

    override fun toString(): String = "krylix.health_bar_style.${name.lowercase()}"
}
