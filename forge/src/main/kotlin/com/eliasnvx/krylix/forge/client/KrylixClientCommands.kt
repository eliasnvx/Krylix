package com.eliasnvx.krylix.forge.client

import com.eliasnvx.krylix.Krylix
import com.eliasnvx.krylix.forge.network.NetworkPackets.PlayerStatsSyncPacket.PlayerStatEntry
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.util.UUID
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
                .then(
                    Commands.literal("hud")
                        .then(
                            Commands.literal("toggle")
                                .executes { context: CommandContext<CommandSourceStack> ->
                                    KillFeedHud.setEnabled(!KillFeedHud.isHudEnabled())
                                    val state = Component.translatable(if (KillFeedHud.isHudEnabled()) "krylix.toggle.on" else "krylix.toggle.off")
                                    context.source.sendSuccess(
                                        Supplier { Component.translatable("krylix.command.hud_state", state) },
                                        true
                                    )
                                    1
                                }
                        )
                        .then(
                            Commands.literal("clear")
                                .executes { context: CommandContext<CommandSourceStack> ->
                                    KillFeedHud.clearNotifications()
                                    context.source.sendSuccess(
                                        Supplier { Component.translatable("krylix.command.hud_cleared") },
                                        true
                                    )
                                    1
                                }
                        )
                        .then(
                            Commands.literal("count")
                                .executes { context: CommandContext<CommandSourceStack> ->
                                    context.source.sendSuccess(
                                        Supplier { Component.translatable("krylix.command.hud_count", KillFeedHud.getNotificationCount()) },
                                        true
                                    )
                                    1
                                }
                        )
                )
                .then(
                    Commands.literal("leaderboard")
                        .then(
                            Commands.literal("testfill")
                                .executes { context: CommandContext<CommandSourceStack> ->
                                    val count = fillTestLeaderboard(60)
                                    context.source.sendSuccess(
                                        Supplier { Component.translatable("krylix.command.leaderboard_filled", count) },
                                        true
                                    )
                                    1
                                }
                                .then(
                                    Commands.argument("count", IntegerArgumentType.integer(1, 500))
                                        .executes { context: CommandContext<CommandSourceStack> ->
                                            val requested = IntegerArgumentType.getInteger(context, "count")
                                            val count = fillTestLeaderboard(requested)
                                            context.source.sendSuccess(
                                                Supplier { Component.translatable("krylix.command.leaderboard_filled", count) },
                                                true
                                            )
                                            1
                                        }
                                )
                        )
                        .then(
                            Commands.literal("clear")
                                .executes { context: CommandContext<CommandSourceStack> ->
                                    PlayerStatsClient.updateStats(emptyList())
                                    context.source.sendSuccess(
                                        Supplier { Component.translatable("krylix.command.leaderboard_cleared") },
                                        true
                                    )
                                    1
                                }
                        )
                )
        )

        Krylix.LOGGER.debug("Krylix client commands registered")
    }

    /**
     * Заполняет клиентский кэш рейтинга случайными данными для проверки пагинации/скролла GUI —
     * чисто визуально, ничего не пишет на сервер и слетит при следующем реальном PlayerStatsSyncPacket
     */
    private fun fillTestLeaderboard(requestedCount: Int): Int {
        val baseNames = listOf(
            "Notch", "Jeb_", "Dinnerbone", "Grumm", "C418", "Herobrine", "Dream", "Technoblade",
            "Ph1LzA", "Tommyinnit", "Wilbur", "Tubbo", "Ranboo", "Sapnap", "GeorgeNotFound",
            "BadBoyHalo", "Skeppy", "Quackity", "Karl_Jacobs", "Fundy", "Purpled", "Antfrost",
            "Awesamdude", "HBomb94", "Ponk", "Vikkstar", "Slimecicle", "Callahan", "ZombieCleo", "Punz"
        )
        val count = requestedCount.coerceIn(1, 500)
        val entries = (0 until count).map { i ->
            val name = baseNames.getOrElse(i) { "TestPlayer${i + 1}" }
            PlayerStatEntry(
                uuid = UUID.randomUUID().toString(),
                name = name,
                kills = (0..60).random(),
                deaths = (0..40).random(),
                mobKills = (0..120).random()
            )
        }
        PlayerStatsClient.updateStats(entries)
        return entries.size
    }
}
