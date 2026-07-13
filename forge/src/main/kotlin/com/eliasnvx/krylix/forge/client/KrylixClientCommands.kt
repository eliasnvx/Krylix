package com.example.modid.forge.client

import com.example.modid.forge.KillFeedManager
import com.example.modid.krylix.Krylix
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.minecraft.client.Minecraft
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.util.function.Supplier

/**
 * Client-side команды для Krylix мода (только HUD)
 */
@Mod.EventBusSubscriber(modid = Krylix.MOD_ID, value = [net.minecraftforge.api.distmarker.Dist.CLIENT])
object KrylixClientCommands {
    
    /**
     * Обработчик регистрации клиентских команд
     */
    @SubscribeEvent
    @JvmStatic
    fun onRegisterCommands(event: RegisterCommandsEvent) {
        val dispatcher: CommandDispatcher<CommandSourceStack> = event.dispatcher
        
        // Добавляем client-only команды к существующей команде /krylix
        dispatcher.register(
            Commands.literal("krylix")
                .then(Commands.literal("hud")
                    .then(Commands.literal("toggle")
                        .executes { context: CommandContext<CommandSourceStack> ->
                            KillFeedHud.setEnabled(!KillFeedHud.isHudEnabled())
                            context.source.sendSuccess(
                                Supplier { Component.literal("Kill feed HUD ${if (KillFeedHud.isHudEnabled()) "enabled" else "disabled"}") },
                                true
                            )
                            1
                        }
                    )
                    .then(Commands.literal("clear")
                        .executes { context: CommandContext<CommandSourceStack> ->
                            KillFeedHud.clearNotifications()
                            context.source.sendSuccess(
                                Supplier { Component.literal("Kill feed HUD cleared") },
                                true
                            )
                            1
                        }
                    )
                )
        )
        
        Krylix.LOGGER.debug("Krylix client commands registered")
    }
}
