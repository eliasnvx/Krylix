package com.example.modid.forge

import com.example.modid.forge.config.ModConfig
import com.example.modid.forge.network.NetworkPackets
import com.example.modid.forge.client.KrylixClient
import com.example.modid.krylix.Krylix
import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.common.Mod
import net.minecraft.world.entity.player.Player

@Mod(Krylix.MOD_ID)
class KrylixForge {
    init {
        Krylix.LOGGER.info("Initializing Krylix Forge mod")
        
        // Регистрация network packets
        NetworkPackets
        
        // Init config screen (только на клиенте)
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient) {
            try {
                AutoConfig.register(ModConfig::class.java, ::JanksonConfigSerializer)
                // Config screen registration будет в отдельном client классе
                
                // Ручная регистрация client-side событий
                KrylixClient.registerClientEvents()
            } catch (e: Exception) {
                Krylix.LOGGER.warn("Failed to register config screen: ${e.message}")
            }
        }

        // Register event bus for server-safe events only
        MinecraftForge.EVENT_BUS.register(this)
        MinecraftForge.EVENT_BUS.register(KrylixServerCommands)
        
        // Initialize common mod code
        Krylix.init()
        
        Krylix.LOGGER.info("Krylix Forge mod initialized successfully")
    }
    
    /**
     * Обработчик события смерти сущности для отслеживания убийств (server-safe)
     */
    @SubscribeEvent
    fun onLivingDeath(event: LivingDeathEvent) {
        val entity = event.entity
        val source = event.source
        
        // Обрабатываем только игроков
        if (entity is Player) {
            KillFeedManager.handleDeath(entity, source)
        }
        
        Krylix.LOGGER.debug("Living death event: ${entity.name.string} died from ${source.msgId}")
    }
}
