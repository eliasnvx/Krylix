package com.eliasnvx.krylix

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

/**
 * Главный объект мода Krylix
 * Kill feed мод в стиле CS:GO/Valorant для Minecraft
 */
object Krylix {
    const val MOD_ID = "krylix"
    const val MOD_NAME = "Krylix"

    val LOGGER: Logger = LogManager.getLogger(MOD_NAME)

    /**
     * Инициализация общей (platform-agnostic) части мода, вызывается из KrylixForge.init
     */
    fun init() {
        LOGGER.info("Initializing $MOD_NAME")
    }
}
