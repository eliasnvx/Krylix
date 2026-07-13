package com.eliasnvx.krylix.forge

import com.eliasnvx.krylix.Krylix
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import java.util.function.Supplier

/**
 * Server-side команды для Krylix мода
 */
object KrylixServerCommands {

    /**
     * Обработчик регистрации команд
     */
    @SubscribeEvent
    fun onRegisterCommands(event: RegisterCommandsEvent) {
        val dispatcher: CommandDispatcher<CommandSourceStack> = event.dispatcher

        // Команда /krylix toggle - включить/выключить kill feed
        dispatcher.register(
            Commands.literal("krylix")
                .then(
                    Commands.literal("toggle")
                        .executes { context: CommandContext<CommandSourceStack> ->
                            KillFeedManager.setEnabled(!KillFeedManager.isEnabled)
                            val state = Component.translatable(if (KillFeedManager.isEnabled) "krylix.toggle.on" else "krylix.toggle.off")
                            context.source.sendSuccess(
                                Supplier { Component.translatable("krylix.command.killfeed_state", state) },
                                true
                            )
                            1
                        }
                )
                .then(
                    Commands.literal("status")
                        .executes { context: CommandContext<CommandSourceStack> ->
                            val serverStatus = Component.translatable(if (KillFeedManager.isEnabled) "krylix.toggle.on" else "krylix.toggle.off")
                            val count = KillFeedManager.getActiveNotifications().size
                            context.source.sendSuccess(
                                Supplier { Component.translatable("krylix.command.status", serverStatus, count) },
                                true
                            )
                            1
                        }
                )
        )

        Krylix.LOGGER.debug("Krylix server commands registered")
    }
}
