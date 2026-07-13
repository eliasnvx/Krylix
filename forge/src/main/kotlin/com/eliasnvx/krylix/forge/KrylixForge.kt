package com.eliasnvx.krylix.forge

import com.eliasnvx.krylix.Krylix
import com.eliasnvx.krylix.forge.client.KrylixClient
import com.eliasnvx.krylix.forge.config.ModConfig
import com.eliasnvx.krylix.forge.network.NetworkPackets
import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.player.Player
import net.minecraftforge.client.ConfigScreenHandler
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.common.Mod

@Mod(Krylix.MOD_ID)
class KrylixForge {
    init {
        Krylix.LOGGER.info("Initializing Krylix Forge mod")

        // Регистрация network packets
        NetworkPackets

        // Конфиг регистрируем на обеих сторонах, чтобы сервер и клиент читали одни и те же тайминги
        try {
            AutoConfig.register(ModConfig::class.java, ::JanksonConfigSerializer)
        } catch (e: Exception) {
            Krylix.LOGGER.warn("Failed to register config: ${e.message}")
        }

        // HUD/рендеринг — только на клиенте
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient) {
            KrylixClient.registerClientEvents()

            // Подключает кнопку "Config" в ванильном списке модов (Меню -> Моды -> Krylix) —
            // AutoConfig сам по себе экран не регистрирует, это нужно сделать явно на стороне Forge
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory::class.java) {
                ConfigScreenHandler.ConfigScreenFactory { _, screen ->
                    AutoConfig.getConfigScreen(ModConfig::class.java, screen).get()
                }
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

        if (entity is Player) {
            KillFeedManager.handleDeath(entity, source)
            handlePlayerKillStats(entity, source)
        } else {
            handleMobKillStats(entity, source)
        }

        Krylix.LOGGER.debug("Living death event: ${entity.name.string} died from ${source.msgId}")
    }

    /**
     * Учитывает PvP-убийство/смерть в per-world рейтинге игроков (см. PlayerKillStatsData)
     */
    private fun handlePlayerKillStats(victim: Player, source: DamageSource) {
        val level = victim.level()
        if (level !is ServerLevel) return

        val statsData = PlayerKillStatsData.get(level.server)
        statsData.recordDeath(victim.uuid.toString(), victim.name.string)

        val killer = source.entity as? Player
        if (killer != null) {
            statsData.recordKill(killer.uuid.toString(), killer.name.string)
        }

        NetworkPackets.broadcastPlayerStatsSync(statsData.stats)
    }

    /**
     * Учитывает убийство враждебного моба игроком в per-world статистике (см. MobKillStatsData)
     */
    private fun handleMobKillStats(entity: LivingEntity, source: DamageSource) {
        val level = entity.level()
        if (level !is ServerLevel) return
        if (entity.type.category != MobCategory.MONSTER) return
        val killer = source.entity as? ServerPlayer ?: return

        val entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.type).toString()
        val kills = MobKillStatsData.get(level.server).increment(entityId)
        NetworkPackets.sendMobStatsSync(killer, kills)

        // Личный счётчик "кто из игроков убил больше мобов" — отдельная вкладка рейтинга
        val playerStats = PlayerKillStatsData.get(level.server)
        playerStats.recordMobKill(killer.uuid.toString(), killer.name.string)
        NetworkPackets.broadcastPlayerStatsSync(playerStats.stats)
    }

    /**
     * Досылает уже накопленную per-world статистику при входе, иначе панель будет казаться
     * пустой до первого килла в новой сессии
     */
    @SubscribeEvent
    fun onPlayerLogin(event: PlayerEvent.PlayerLoggedInEvent) {
        val player = event.entity as? ServerPlayer ?: return
        val level = player.level() as? ServerLevel ?: return
        NetworkPackets.sendMobStatsSync(player, MobKillStatsData.get(level.server).kills)
        NetworkPackets.sendPlayerStatsSync(player, PlayerKillStatsData.get(level.server).stats)
    }
}
