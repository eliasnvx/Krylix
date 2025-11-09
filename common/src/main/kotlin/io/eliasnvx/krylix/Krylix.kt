package io.eliasnvx.krylix

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

/**
 * Главный объект мода Krylix
 * Kill feed мод в стиле CS:GO/Valorant для Minecraft
 */
object Krylix {
    const val MOD_ID = "krylix"
    const val MOD_NAME = "Krylix"
    const val MOD_VERSION = "1.0.0"

    val LOGGER: Logger = LogManager.getLogger(MOD_NAME)

    /**
     * Инициализация мода
     * Вызывается из platform-specific entry points (Fabric/Forge)
     */
    fun init() {
        LOGGER.info("Initializing $MOD_NAME v$MOD_VERSION")

        // TODO: Регистрация событий
        // TODO: Загрузка конфига

        LOGGER.info("$MOD_NAME initialized successfully")
    }
}
