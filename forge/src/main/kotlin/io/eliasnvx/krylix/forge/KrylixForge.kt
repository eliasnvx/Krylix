package io.eliasnvx.krylix.forge

import dev.architectury.platform.forge.EventBuses
import io.eliasnvx.krylix.Krylix
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext

/**
 * Forge entry point для Krylix
 * Client-side only мод
 */
@Mod(Krylix.MOD_ID)
class KrylixForge {
    init {
        Krylix.LOGGER.info("Initializing Krylix on Forge")

        // Получаем mod event bus
        val modEventBus = FMLJavaModLoadingContext.get().modEventBus

        // Регистрация Architectury event bus
        EventBuses.registerModEventBus(Krylix.MOD_ID, modEventBus)

        // Client setup
        modEventBus.addListener(::onClientSetup)

        Krylix.LOGGER.info("Krylix Forge initialization complete")
    }

    private fun onClientSetup(event: FMLClientSetupEvent) {
        event.enqueueWork {
            // Инициализация common кода
            Krylix.init()

            // TODO: Регистрация Forge-specific событий
            // TODO: Регистрация key bindings
            // TODO: Регистрация HUD renderer
        }
    }
}
