package com.eliasnvx.krylix.fabric;

import com.eliasnvx.krylix.Krylix;
import com.eliasnvx.krylix.fabric.config.ModConfig;
import com.eliasnvx.krylix.fabric.network.FabricNetworkPackets;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KrylixFabric implements ModInitializer {
    private final Map<String, Map<String, CombatDamage>> combatDamageMap = new HashMap<>();

    private static class CombatDamage {
        long timestamp;
        float totalDamage;
        CombatDamage(long ts, float dmg) { this.timestamp = ts; this.totalDamage = dmg; }
    }

    @Override
    public void onInitialize() {
        Krylix.LOGGER.info("Initializing Krylix Fabric mod");

        try {
            AutoConfig.register(ModConfig.class, JanksonConfigSerializer::new);
        } catch (Exception e) {
            Krylix.LOGGER.warn("Failed to register config: " + e.getMessage());
        }

        FabricNetworkPackets.registerPayloads();

        CommandRegistrationCallback.EVENT.register(KrylixServerCommands::register);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            ServerLevel level = (ServerLevel) player.level();
            FabricNetworkPackets.sendToPlayer(player, new FabricNetworkPackets.MobStatsSyncPacket(MobKillStatsData.get(server).kills));

            PlayerKillStatsData playerStats = PlayerKillStatsData.get(server);
            List<FabricNetworkPackets.PlayerStatEntry> entries = new ArrayList<>();
            for (Map.Entry<String, PlayerStat> e : playerStats.stats.entrySet()) {
                entries.add(new FabricNetworkPackets.PlayerStatEntry(e.getKey(), e.getValue().lastName, e.getValue().kills, e.getValue().deaths, e.getValue().mobKills));
            }
            FabricNetworkPackets.sendToPlayer(player, new FabricNetworkPackets.PlayerStatsSyncPacket(entries));
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity.level().isClientSide()) return;

            if (entity instanceof ServerPlayer sp) {
                sendDeathRecap(sp, source);
            }

            if (entity instanceof Player p) {
                KillFeedManager.handleDeath(p, source);
                handlePlayerKillStats(p, source);
            } else {
                handleMobKillStats(entity, source);
            }
        });

        Krylix.init();
        Krylix.LOGGER.info("Krylix Fabric mod initialized successfully");
    }

    private void sendDeathRecap(ServerPlayer victim, DamageSource source) {
        LivingEntity killerEntity = source.getEntity() instanceof LivingEntity ? (LivingEntity) source.getEntity() : null;
        String killerName = killerEntity != null ? killerEntity.getName().getString() : source.getLocalizedDeathMessage(victim).getString();
        String killerUUID = killerEntity instanceof Player p ? p.getUUID().toString() : null;
        float killerHealth = killerEntity != null ? killerEntity.getHealth() : 0f;
        float killerMaxHealth = killerEntity != null ? killerEntity.getMaxHealth() : 20f;
        String weapon = "minecraft:air";
        if (killerEntity instanceof Player p && !p.getMainHandItem().isEmpty()) {
            weapon = BuiltInRegistries.ITEM.getKey(p.getMainHandItem().getItem()).toString();
        }
        Double distance = killerEntity != null ? Math.sqrt(killerEntity.distanceToSqr(victim)) : null;

        boolean isSmash = "mace_smash".equals(source.type().msgId()) ||
            (killerEntity instanceof Player p && "minecraft:mace".equals(weapon) && p.fallDistance > 1.0f);
        boolean isCrit = killerEntity instanceof Player p && p.fallDistance > 0.0f && !p.onGround();
        double distVal = distance != null ? distance : 0.0;
        boolean isLongshot = distVal >= 30.0 && (weapon.contains("bow") || weapon.contains("trident") || weapon.contains("arrow"));

        String victimId = victim.getUUID().toString();
        String targetId = killerUUID != null ? killerUUID : (killerEntity != null ? killerEntity.getUUID().toString() : "");
        float damageDealt = 0f;
        Map<String, CombatDamage> vm = combatDamageMap.get(victimId);
        if (vm != null) {
            CombatDamage cd = vm.get(targetId);
            if (cd != null && System.currentTimeMillis() - cd.timestamp < 20_000) {
                damageDealt = cd.totalDamage;
            }
        }

        FabricNetworkPackets.DeathRecapPacket recap = new FabricNetworkPackets.DeathRecapPacket(
            killerName, killerUUID, killerHealth, killerMaxHealth, weapon, distance, damageDealt, isSmash, isCrit, isLongshot
        );
        FabricNetworkPackets.sendToPlayer(victim, recap);
    }

    private void handlePlayerKillStats(Player victim, DamageSource source) {
        if (!(victim.level() instanceof ServerLevel level)) return;

        PlayerKillStatsData statsData = PlayerKillStatsData.get(level.getServer());
        statsData.recordDeath(victim.getUUID().toString(), victim.getName().getString());

        if (source.getEntity() instanceof Player killer) {
            statsData.recordKill(killer.getUUID().toString(), killer.getName().getString());
        }

        List<FabricNetworkPackets.PlayerStatEntry> entries = new ArrayList<>();
        for (Map.Entry<String, PlayerStat> e : statsData.stats.entrySet()) {
            entries.add(new FabricNetworkPackets.PlayerStatEntry(e.getKey(), e.getValue().lastName, e.getValue().kills, e.getValue().deaths, e.getValue().mobKills));
        }
        FabricNetworkPackets.sendToAll(level, new FabricNetworkPackets.PlayerStatsSyncPacket(entries));
    }

    private void handleMobKillStats(LivingEntity entity, DamageSource source) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        if (entity.getType().getCategory() != MobCategory.MONSTER) return;
        if (!(source.getEntity() instanceof ServerPlayer killer)) return;

        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        Map<String, Integer> kills = MobKillStatsData.get(level.getServer()).increment(entityId);
        FabricNetworkPackets.sendToPlayer(killer, new FabricNetworkPackets.MobStatsSyncPacket(kills));

        PlayerKillStatsData playerStats = PlayerKillStatsData.get(level.getServer());
        playerStats.recordMobKill(killer.getUUID().toString(), killer.getName().getString());

        List<FabricNetworkPackets.PlayerStatEntry> entries = new ArrayList<>();
        for (Map.Entry<String, PlayerStat> e : playerStats.stats.entrySet()) {
            entries.add(new FabricNetworkPackets.PlayerStatEntry(e.getKey(), e.getValue().lastName, e.getValue().kills, e.getValue().deaths, e.getValue().mobKills));
        }
        FabricNetworkPackets.sendToAll(level, new FabricNetworkPackets.PlayerStatsSyncPacket(entries));
    }
}
