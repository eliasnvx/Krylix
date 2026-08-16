package com.eliasnvx.krylix.fabric;

import com.eliasnvx.krylix.Krylix;
import com.eliasnvx.krylix.fabric.config.KrylixConfig;
import com.eliasnvx.krylix.fabric.network.FabricNetworkPackets;
import com.eliasnvx.krylix.model.KillEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class KillFeedManager {
    private static final List<KillEntry> activeNotifications = new ArrayList<>();
    private static boolean isEnabled = true;

    public static void handleDeath(LivingEntity entity, DamageSource source) {
        if (!isEnabled || entity.level().isClientSide) return;

        String victimName = entity.getName().getString();
        String victimUUIDString = null;
        if (entity instanceof Player player) {
            victimUUIDString = player.getUUID().toString();
        }

        LivingEntity killerEntity = source.getEntity() instanceof LivingEntity ? (LivingEntity) source.getEntity() : null;
        String killerName = killerEntity != null ? killerEntity.getName().getString() : null;
        String killerUUIDString = null;
        float killerHealth = 20.0f;
        if (killerEntity instanceof Player player) {
            killerUUIDString = player.getUUID().toString();
            killerHealth = player.getHealth();
        }

        String weaponId = getWeaponId(source);
        Double distance = calculateDistance(killerEntity, entity);

        boolean isSmash = "mace_smash".equals(source.type().msgId()) ||
                (killerEntity instanceof Player player && "minecraft:mace".equals(weaponId) && player.fallDistance > 1.0f);
        boolean isCrit = killerEntity instanceof Player player && player.fallDistance > 0.0f &&
                !player.onGround() && !player.onClimbable() && !player.isInWater();

        KillEntry killEntry = new KillEntry(
                killerName,
                killerUUIDString,
                victimName,
                victimUUIDString,
                killerHealth,
                weaponId,
                distance,
                System.currentTimeMillis(),
                false,
                isSmash,
                isCrit
        );

        activeNotifications.add(killEntry);
        Krylix.LOGGER.info("Kill registered: " + killEntry.getKillerName() + " killed " + killEntry.getVictimName());

        broadcastKillNotification(killEntry, (ServerLevel) entity.level());
        cleanupOldNotifications();
    }

    private static String getWeaponId(DamageSource source) {
        if (source.getDirectEntity() instanceof Player player) {
            ItemStack weapon = player.getMainHandItem();
            if (weapon.isEmpty()) return "minecraft:air";
            return BuiltInRegistries.ITEM.getKey(weapon.getItem()).toString();
        }
        String msgId = source.type().msgId();
        if (msgId.contains("arrow")) return "minecraft:arrow";
        if (msgId.contains("trident")) return "minecraft:trident";
        if (msgId.contains("fireball") || msgId.contains("fire")) return "minecraft:fire_charge";
        if (msgId.contains("magic")) return "minecraft:enchanted_book";
        if (msgId.contains("explosion")) return "minecraft:tnt";
        if (msgId.contains("fall")) return "minecraft:feather";
        return "minecraft:air";
    }

    private static Double calculateDistance(LivingEntity attacker, LivingEntity victim) {
        if (attacker == null) return null;
        return Math.sqrt(attacker.distanceToSqr(victim));
    }

    private static void broadcastKillNotification(KillEntry killEntry, ServerLevel level) {
        FabricNetworkPackets.KillNotificationPacket packet = new FabricNetworkPackets.KillNotificationPacket(
                killEntry.getKillerName(),
                killEntry.getKillerUUIDString(),
                killEntry.getVictimName(),
                killEntry.getVictimUUIDString(),
                killEntry.getKillerHealth(),
                killEntry.getWeaponName(),
                killEntry.getDistance(),
                killEntry.getTimestamp(),
                killEntry.isHeadshot(),
                killEntry.isSmash(),
                killEntry.isCritical()
        );

        if (KrylixConfig.get().restrictBroadcastToSameDimension) {
            FabricNetworkPackets.sendToDimension(level, packet);
        } else {
            FabricNetworkPackets.sendToAll(level, packet);
        }
    }

    private static void cleanupOldNotifications() {
        int displaySeconds = KrylixConfig.get().displaySeconds;
        activeNotifications.removeIf(entry -> entry.isExpired(displaySeconds));
    }

    public static void setEnabled(boolean enabled) {
        isEnabled = enabled;
        Krylix.LOGGER.info("Kill feed " + (enabled ? "enabled" : "disabled"));
    }

    public static boolean isEnabled() {
        return isEnabled;
    }

    public static List<KillEntry> getActiveNotifications() {
        return new ArrayList<>(activeNotifications);
    }
}
