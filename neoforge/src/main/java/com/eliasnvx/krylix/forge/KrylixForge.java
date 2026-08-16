package com.eliasnvx.krylix.forge;

import com.eliasnvx.krylix.Krylix;
import com.eliasnvx.krylix.forge.config.ModConfig;
import com.eliasnvx.krylix.forge.network.NetworkPackets;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod(Krylix.MOD_ID)
public class KrylixForge {
    private final Map<String, Map<String, CombatDamage>> combatDamageMap = new HashMap<>();

    public KrylixForge(IEventBus modEventBus, ModContainer container) {
        Krylix.LOGGER.info("Initializing Krylix NeoForge mod");

        modEventBus.addListener(NetworkPackets::register);

        try {
            AutoConfig.register(ModConfig.class, JanksonConfigSerializer::new);
        } catch (Exception e) {
            Krylix.LOGGER.warn("Failed to register config: " + e.getMessage());
        }

        if (FMLEnvironment.dist.isClient()) {
            com.eliasnvx.krylix.forge.client.KrylixClient.registerClientEvents();
            container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (mc, screen) -> AutoConfig.getConfigScreen(ModConfig.class, screen).get()
            );
        }

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(KrylixServerCommands.class);

        Krylix.init();
        Krylix.LOGGER.info("Krylix NeoForge mod initialized successfully");
    }

    private static class CombatDamage {
        long timestamp;
        float totalDamage;
        CombatDamage(long ts, float dmg) { this.timestamp = ts; this.totalDamage = dmg; }
    }

    @SubscribeEvent
    public void onLivingDamage(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        LivingEntity target = event.getEntity();
        String attackerId = attacker.getUUID().toString();
        String targetId = target instanceof Player p ? p.getUUID().toString() : target.getUUID().toString();

        long now = System.currentTimeMillis();
        Map<String, CombatDamage> targetMap = combatDamageMap.computeIfAbsent(attackerId, k -> new HashMap<>());
        CombatDamage prev = targetMap.get(targetId);
        float total = (prev != null && now - prev.timestamp < 20_000) ? prev.totalDamage + event.getNewDamage() : event.getNewDamage();
        targetMap.put(targetId, new CombatDamage(now, total));
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        DamageSource source = event.getSource();

        if (entity instanceof ServerPlayer sp) {
            sendDeathRecap(sp, source);
        }

        if (entity instanceof Player p) {
            KillFeedManager.handleDeath(p, source);
            handlePlayerKillStats(p, source);
        } else {
            handleMobKillStats(entity, source);
        }

        Krylix.LOGGER.debug("Living death event: " + entity.getName().getString() + " died from " + source.type().msgId());
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

        NetworkPackets.DeathRecapPacket recap = new NetworkPackets.DeathRecapPacket(
            killerName, killerUUID, killerHealth, killerMaxHealth, weapon, distance, damageDealt, isSmash, isCrit, isLongshot
        );
        NetworkPackets.sendToPlayer(victim, recap);
    }

    private void handlePlayerKillStats(Player victim, DamageSource source) {
        if (!(victim.level() instanceof ServerLevel level)) return;

        PlayerKillStatsData statsData = PlayerKillStatsData.get(level.getServer());
        statsData.recordDeath(victim.getUUID().toString(), victim.getName().getString());

        if (source.getEntity() instanceof Player killer) {
            statsData.recordKill(killer.getUUID().toString(), killer.getName().getString());
        }

        List<NetworkPackets.PlayerStatEntry> entries = new ArrayList<>();
        for (Map.Entry<String, PlayerStat> e : statsData.stats.entrySet()) {
            entries.add(new NetworkPackets.PlayerStatEntry(e.getKey(), e.getValue().lastName, e.getValue().kills, e.getValue().deaths, e.getValue().mobKills));
        }
        NetworkPackets.sendToAll(new NetworkPackets.PlayerStatsSyncPacket(entries));
    }

    private void handleMobKillStats(LivingEntity entity, DamageSource source) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        if (entity.getType().getCategory() != MobCategory.MONSTER) return;
        if (!(source.getEntity() instanceof ServerPlayer killer)) return;

        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        Map<String, Integer> kills = MobKillStatsData.get(level.getServer()).increment(entityId);
        NetworkPackets.sendToPlayer(killer, new NetworkPackets.MobStatsSyncPacket(kills));

        PlayerKillStatsData playerStats = PlayerKillStatsData.get(level.getServer());
        playerStats.recordMobKill(killer.getUUID().toString(), killer.getName().getString());
        
        List<NetworkPackets.PlayerStatEntry> entries = new ArrayList<>();
        for (Map.Entry<String, PlayerStat> e : playerStats.stats.entrySet()) {
            entries.add(new NetworkPackets.PlayerStatEntry(e.getKey(), e.getValue().lastName, e.getValue().kills, e.getValue().deaths, e.getValue().mobKills));
        }
        NetworkPackets.sendToAll(new NetworkPackets.PlayerStatsSyncPacket(entries));
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        NetworkPackets.sendToPlayer(player, new NetworkPackets.MobStatsSyncPacket(MobKillStatsData.get(level.getServer()).kills));
        
        List<NetworkPackets.PlayerStatEntry> entries = new ArrayList<>();
        for (Map.Entry<String, PlayerStat> e : PlayerKillStatsData.get(level.getServer()).stats.entrySet()) {
            entries.add(new NetworkPackets.PlayerStatEntry(e.getKey(), e.getValue().lastName, e.getValue().kills, e.getValue().deaths, e.getValue().mobKills));
        }
        NetworkPackets.sendToPlayer(player, new NetworkPackets.PlayerStatsSyncPacket(entries));
    }
}
