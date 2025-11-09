package io.eliasnvx.krylix.fabric

import io.eliasnvx.krylix.Krylix
import net.fabricmc.api.ClientModInitializer

/**
 * Fabric entry point для Krylix
 * Client-side only мод
 */
class KrylixFabric : ClientModInitializer {
    override fun onInitializeClient() {
        Krylix.LOGGER.info("Initializing Krylix on Fabric")

        // Инициализация common кода
        Krylix.init()

        // TODO: Регистрация Fabric-specific событий
        // TODO: Регистрация key bindings
        // TODO: Регистрация HUD renderer

        Krylix.LOGGER.info("Krylix Fabric initialization complete")
    }
}
