package com.eliasnvx.krylix.forge.client

import com.eliasnvx.krylix.Krylix
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RegisterKeyMappingsEvent
import net.minecraftforge.client.settings.KeyConflictContext
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import org.lwjgl.glfw.GLFW

/**
 * Клавиши быстрого вкл/выкл kill feed, панели статистики мобов, открытия рейтинга игроков и
 * индикатора здоровья под прицелом. K/L/O/H свободны в ванильной раскладке — при конфликте с
 * другим модом можно перебиндить в Controls.
 */
object KrylixKeyBindings {
    val toggleKillFeed = KeyMapping(
        "key.krylix.toggle_killfeed",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_K),
        "key.categories.krylix"
    )

    val toggleMobStats = KeyMapping(
        "key.krylix.toggle_mobstats",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_L),
        "key.categories.krylix"
    )

    val openLeaderboard = KeyMapping(
        "key.krylix.open_leaderboard",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_O),
        "key.categories.krylix"
    )

    val toggleHealthIndicator = KeyMapping(
        "key.krylix.toggle_health_indicator",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_H),
        "key.categories.krylix"
    )

    @Mod.EventBusSubscriber(modid = Krylix.MOD_ID, value = [Dist.CLIENT], bus = Mod.EventBusSubscriber.Bus.MOD)
    object ModBusEvents {
        @SubscribeEvent
        @JvmStatic
        fun onRegisterKeyMappings(event: RegisterKeyMappingsEvent) {
            event.register(toggleKillFeed)
            event.register(toggleMobStats)
            event.register(openLeaderboard)
            event.register(toggleHealthIndicator)
        }
    }
}
