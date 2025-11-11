package com.example.modid.forge.client

import com.example.modid.krylix.Krylix
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.util.*

/**
 * Серверные команды для тестирования Kill Feed HUD
 */
@Mod.EventBusSubscriber(modid = Krylix.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object KillFeedTestCommands {
    
    @SubscribeEvent
    @JvmStatic
    fun registerCommands(event: RegisterCommandsEvent) {
        Krylix.LOGGER.info("Registering test commands for kill feed HUD")
        val dispatcher: CommandDispatcher<CommandSourceStack> = event.dispatcher
        
        // Основная команда /testkill
        dispatcher.register(Commands.literal("testkill")
            .executes { context ->
                addRandomTestKill(context.source)
                1
            }
            .then(Commands.literal("sword")
                .executes { context ->
                    addSpecificTestKill(context.source, "Diamond Sword", 20.0f, 3.0)
                    1
                })
            .then(Commands.literal("bow")
                .executes { context ->
                    addSpecificTestKill(context.source, "Bow", 18.5f, 15.0)
                    1
                })
            .then(Commands.literal("axe")
                .executes { context ->
                    addSpecificTestKill(context.source, "Iron Axe", 16.0f, 2.0)
                    1
                })
            .then(Commands.literal("headshot")
                .executes { context ->
                    addHeadshotTestKill(context.source)
                    1
                })
            .then(Commands.literal("multi")
                .executes { context ->
                    addMultipleTestKills(context.source)
                    1
                })
            .then(Commands.literal("clear")
                .executes { context ->
                    KillFeedHud.clearNotifications()
                    context.source.sendSuccess({ Component.literal("Kill feed cleared!") }, true)
                    1
                })
            .then(Commands.literal("count")
                .executes { context ->
                    val count = KillFeedHud.getNotificationCount()
                    context.source.sendSuccess({ Component.literal("Active notifications: $count") }, true)
                    1
                })
        )
    }
    
    private fun addRandomTestKill(source: CommandSourceStack) {
        val weapons = arrayOf("Diamond Sword", "Iron Axe", "Bow", "Crossbow", "Trident")
        val killers = arrayOf("Steve", "Alex", "Herobrine", "Notch", "Jeb_")
        val victims = arrayOf("Zombie", "Skeleton", "Creeper", "Enderman", "Player")
        
        val weapon = weapons.random()
        val killer = killers.random()
        val victim = victims.random()
        val hp = (10..20).random().toFloat()
        val distance = (1..20).random().toDouble()
        
        val testEntry = com.example.modid.krylix.model.KillEntry(
            killerName = killer,
            killerUUIDString = UUID.randomUUID().toString(),
            victimName = victim,
            victimUUIDString = UUID.randomUUID().toString(),
            killerHealth = hp,
            weaponName = weapon,
            distance = distance,
            isHeadshot = false
        )
        
        KillFeedHud.addNotification(testEntry)
        source.sendSuccess({ Component.literal("Added test kill: $killer killed $victim with $weapon") }, true)
    }
    
    private fun addSpecificTestKill(source: CommandSourceStack, weapon: String, hp: Float, distance: Double) {
        val testEntry = com.example.modid.krylix.model.KillEntry(
            killerName = "TestKiller",
            killerUUIDString = UUID.randomUUID().toString(),
            victimName = "TestVictim",
            victimUUIDString = UUID.randomUUID().toString(),
            killerHealth = hp,
            weaponName = weapon,
            distance = distance,
            isHeadshot = false
        )
        
        KillFeedHud.addNotification(testEntry)
        source.sendSuccess({ Component.literal("Added $weapon test kill (HP: $hp, Distance: ${distance}m)") }, true)
    }
    
    private fun addHeadshotTestKill(source: CommandSourceStack) {
        val testEntry = com.example.modid.krylix.model.KillEntry(
            killerName = "Sniper",
            killerUUIDString = UUID.randomUUID().toString(),
            victimName = "Target",
            victimUUIDString = UUID.randomUUID().toString(),
            killerHealth = 19.5f,
            weaponName = "Bow",
            distance = 50.0,
            isHeadshot = true
        )
        
        KillFeedHud.addNotification(testEntry)
        source.sendSuccess({ Component.literal("Added headshot test kill!") }, true)
    }
    
    private fun addMultipleTestKills(source: CommandSourceStack) {
        val kills = listOf(
            com.example.modid.krylix.model.KillEntry(
                killerName = "Player1",
                killerUUIDString = UUID.randomUUID().toString(),
                victimName = "Player2",
                victimUUIDString = UUID.randomUUID().toString(),
                killerHealth = 20.0f,
                weaponName = "Diamond Sword",
                distance = 2.0
            ),
            com.example.modid.krylix.model.KillEntry(
                killerName = "Player3",
                killerUUIDString = UUID.randomUUID().toString(),
                victimName = "Player4",
                victimUUIDString = UUID.randomUUID().toString(),
                killerHealth = 15.5f,
                weaponName = "Bow",
                distance = 25.0
            ),
            com.example.modid.krylix.model.KillEntry(
                killerName = "Player5",
                killerUUIDString = UUID.randomUUID().toString(),
                victimName = "Player6",
                victimUUIDString = UUID.randomUUID().toString(),
                killerHealth = 8.0f,
                weaponName = "Iron Axe",
                distance = 1.5
            )
        )
        
        kills.forEach { KillFeedHud.addNotification(it) }
        source.sendSuccess({ Component.literal("Added 3 test kills for multi-line testing!") }, true)
    }
}
