package com.example.modid.forge

import com.example.modid.forge.config.ModConfig
import com.example.modid.krylix.Krylix
import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraftforge.client.ConfigScreenHandler
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.common.Mod
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import java.util.function.Supplier

@Mod(Krylix.MOD_ID)
class KrylixForge {
    init {
        Krylix.LOGGER.info("Initializing Krylix Forge mod")
        
        // Init config screen
        AutoConfig.register(ModConfig::class.java, ::JanksonConfigSerializer)
        ModLoadingContext
            .get()
            .registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory::class.java) {
                ConfigScreenHandler.ConfigScreenFactory { _: Minecraft?, parent: Screen? ->
                    AutoConfig.getConfigScreen(
                        ModConfig::class.java,
                        parent,
                    ).get()
                }
            }

        // Register event bus for game events
        MinecraftForge.EVENT_BUS.register(this)
        
        // Initialize common mod code
        Krylix.init()
        
        Krylix.LOGGER.info("Krylix Forge mod initialized successfully")
    }
    
    /**
     * Обработчик события смерти сущности для отслеживания убийств
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
    
    /**
     * Обработчик регистрации команд
     */
    @SubscribeEvent
    fun onRegisterCommands(event: RegisterCommandsEvent) {
        val dispatcher: CommandDispatcher<CommandSourceStack> = event.dispatcher
        
        // Команда /krylix toggle - включить/выключить kill feed
        dispatcher.register(
            Commands.literal("krylix")
                .then(Commands.literal("toggle")
                    .executes { context: CommandContext<CommandSourceStack> ->
                        KillFeedManager.setEnabled(!KillFeedManager.isEnabled)
                        context.source.sendSuccess(
                            Supplier { Component.literal("Kill feed ${if (KillFeedManager.isEnabled) "enabled" else "disabled"}") },
                            true
                        )
                        1
                    }
                )
                .then(Commands.literal("status")
                    .executes { context: CommandContext<CommandSourceStack> ->
                        val status = if (KillFeedManager.isEnabled) "enabled" else "disabled"
                        val count = KillFeedManager.getActiveNotifications().size
                        context.source.sendSuccess(
                            Supplier { Component.literal("Kill feed status: $status, Active notifications: $count") },
                            true
                        )
                        1
                    }
                )
        )
        
        Krylix.LOGGER.debug("Krylix commands registered")
    }
}
