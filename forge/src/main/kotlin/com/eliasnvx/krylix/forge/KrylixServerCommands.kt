package com.example.modid.forge

import com.example.modid.krylix.Krylix
import com.example.modid.krylix.model.KillEntry
import com.example.modid.forge.network.NetworkPackets
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import java.util.UUID
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
                        val serverStatus = if (KillFeedManager.isEnabled) "enabled" else "disabled"
                        val count = KillFeedManager.getActiveNotifications().size
                        context.source.sendSuccess(
                            Supplier { Component.literal("Kill feed status: Server=$serverStatus, Active notifications: $count") },
                            true
                        )
                        1
                    }
                )
        )

        // Команда /testkill - серверная регистрация для интегрированного и выделенного сервера
        dispatcher.register(
            Commands.literal("testkill")
                .executes { context: CommandContext<CommandSourceStack> ->
                    // Реальные UUID известных игроков Minecraft
                    val killerNames = listOf("Notch", "Jeb_", "Alex", "Herobrine")
                    
                    // Все враждебные мобы
                    val allMobs = listOf(
                        "Zombie", "Skeleton", "Creeper", "Spider", "Enderman",
                        "Piglin", "Zombified Piglin", "Wither Skeleton", "Stray", "Husk",
                        "Drowned", "Blaze", "Ghast", "Slime", "Magma Cube",
                        "Witch", "Pillager", "Vindicator", "Evoker"
                    )
                    
                    val weapons = listOf(
                        "Diamond Sword", "Iron Axe", "Bow", "Crossbow", "Trident",
                        "Netherite Sword", "Golden Axe", "Stone Sword"
                    )
                    
                    // UUID известных игроков (будут загружены скины)
                    val knownUUIDs = mapOf(
                        "Notch" to "069a79f4-44e9-4726-a5be-fca90e38aaf5",
                        "Jeb_" to "853c80ef-3c37-49fd-aa49-938b674adae6",
                        "Alex" to "ec561538-f3fd-461d-aff5-086b22154bce",
                        "Herobrine" to "f84c6a79-0a4e-45e0-879b-cd49ebd4c4e2"
                    )
                    
                    // Случайный выбор: игрок убивает моба или моб убивает игрока
                    val isPlayerKiller = (0..1).random() == 0
                    
                    val entry = if (isPlayerKiller) {
                        val killerName = killerNames.random()
                        val victimName = allMobs.random()
                        val weapon = weapons.random()
                        KillEntry(
                            killerName = killerName,
                            killerUUIDString = knownUUIDs[killerName],
                            victimName = victimName,
                            victimUUIDString = null,
                            killerHealth = (10..20).random().toFloat(),
                            weaponName = weapon,
                            distance = (1..30).random().toDouble()
                        )
                    } else {
                        // Моб убивает игрока
                        val mobName = allMobs.random()
                        val victimName = killerNames.random()
                        KillEntry(
                            killerName = mobName,
                            killerUUIDString = null,
                            victimName = victimName,
                            victimUUIDString = knownUUIDs[victimName],
                            killerHealth = (5..20).random().toFloat(),
                            weaponName = "Melee",
                            distance = (1..5).random().toDouble()
                        )
                    }
                    
                    NetworkPackets.sendKillNotificationToAll(entry)
                    context.source.sendSuccess(Supplier { Component.literal("Added test kill: ${entry.killerName} killed ${entry.victimName}") }, true)
                    1
                }
                .then(Commands.literal("sword").executes { context: CommandContext<CommandSourceStack> ->
                    val entry = KillEntry(
                        killerName = "Notch",
                        killerUUIDString = "069a79f4-44e9-4726-a5be-fca90e38aaf5",
                        victimName = "Zombie",
                        victimUUIDString = null,
                        killerHealth = 20.0f,
                        weaponName = "Diamond Sword",
                        distance = 2.0
                    )
                    NetworkPackets.sendKillNotificationToAll(entry)
                    context.source.sendSuccess(Supplier { Component.literal("Added sword test kill") }, true)
                    1
                })
                .then(Commands.literal("bow").executes { context: CommandContext<CommandSourceStack> ->
                    val entry = KillEntry(
                        killerName = "Jeb_",
                        killerUUIDString = "853c80ef-3c37-49fd-aa49-938b674adae6",
                        victimName = "Skeleton",
                        victimUUIDString = null,
                        killerHealth = 18.5f,
                        weaponName = "Bow",
                        distance = 25.0
                    )
                    NetworkPackets.sendKillNotificationToAll(entry)
                    context.source.sendSuccess(Supplier { Component.literal("Added bow test kill") }, true)
                    1
                })
                .then(Commands.literal("axe").executes { context: CommandContext<CommandSourceStack> ->
                    val entry = KillEntry(
                        killerName = "Alex",
                        killerUUIDString = "ec561538-f3fd-461d-aff5-086b22154bce",
                        victimName = "Creeper",
                        victimUUIDString = null,
                        killerHealth = 16.0f,
                        weaponName = "Iron Axe",
                        distance = 1.5
                    )
                    NetworkPackets.sendKillNotificationToAll(entry)
                    context.source.sendSuccess(Supplier { Component.literal("Added axe test kill") }, true)
                    1
                })
                .then(Commands.literal("headshot").executes { context: CommandContext<CommandSourceStack> ->
                    val entry = KillEntry(
                        killerName = "Herobrine",
                        killerUUIDString = "f84c6a79-0a4e-45e0-879b-cd49ebd4c4e2",
                        victimName = "Enderman",
                        victimUUIDString = null,
                        killerHealth = 19.5f,
                        weaponName = "Bow",
                        distance = 50.0,
                        isHeadshot = true
                    )
                    NetworkPackets.sendKillNotificationToAll(entry)
                    context.source.sendSuccess(Supplier { Component.literal("Added headshot test kill") }, true)
                    1
                })
                .then(Commands.literal("multi").executes { context: CommandContext<CommandSourceStack> ->
                    val kills = listOf(
                        KillEntry(
                            killerName = "Notch",
                            killerUUIDString = "069a79f4-44e9-4726-a5be-fca90e38aaf5",
                            victimName = "Zombie",
                            victimUUIDString = null,
                            killerHealth = 20.0f,
                            weaponName = "Diamond Sword",
                            distance = 2.0
                        ),
                        KillEntry(
                            killerName = "Jeb_",
                            killerUUIDString = "853c80ef-3c37-49fd-aa49-938b674adae6",
                            victimName = "Skeleton",
                            victimUUIDString = null,
                            killerHealth = 15.5f,
                            weaponName = "Bow",
                            distance = 25.0
                        ),
                        KillEntry(
                            killerName = "Alex",
                            killerUUIDString = "ec561538-f3fd-461d-aff5-086b22154bce",
                            victimName = "Creeper",
                            victimUUIDString = null,
                            killerHealth = 8.0f,
                            weaponName = "Iron Axe",
                            distance = 1.5
                        )
                    )
                    kills.forEach { NetworkPackets.sendKillNotificationToAll(it) }
                    context.source.sendSuccess(Supplier { Component.literal("Added 3 test kills for multi-line testing") }, true)
                    1
                })
        )
        
        Krylix.LOGGER.debug("Krylix server commands registered")
    }
}
