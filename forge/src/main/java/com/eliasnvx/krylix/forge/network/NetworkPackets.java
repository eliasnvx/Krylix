package com.eliasnvx.krylix.forge.network;

import com.eliasnvx.krylix.Krylix;
import com.eliasnvx.krylix.model.KillEntry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkPackets {
    public static final SimpleChannel CHANNEL = ChannelBuilder.named(
        ResourceLocation.fromNamespaceAndPath(Krylix.MOD_ID, "main")
    ).networkProtocolVersion(1)
     .simpleChannel();

    public static void register() {
        CHANNEL.messageBuilder(KillNotificationPacket.class)
            .encoder(KillNotificationPacket::encode)
            .decoder(KillNotificationPacket::decode)
            .consumerMainThread((msg, ctx) -> msg.handleOnClient())
            .add();

        CHANNEL.messageBuilder(DeathRecapPacket.class)
            .encoder(DeathRecapPacket::encode)
            .decoder(DeathRecapPacket::decode)
            .consumerMainThread((msg, ctx) -> msg.handleOnClient())
            .add();

        CHANNEL.messageBuilder(MobStatsSyncPacket.class)
            .encoder(MobStatsSyncPacket::encode)
            .decoder(MobStatsSyncPacket::decode)
            .consumerMainThread((msg, ctx) -> msg.handleOnClient())
            .add();

        CHANNEL.messageBuilder(PlayerStatsSyncPacket.class)
            .encoder(PlayerStatsSyncPacket::encode)
            .decoder(PlayerStatsSyncPacket::decode)
            .consumerMainThread((msg, ctx) -> msg.handleOnClient())
            .add();
    }

    public static void sendToAll(Object packet) {
        CHANNEL.send(packet, PacketDistributor.ALL.noArg());
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(packet, PacketDistributor.PLAYER.with(player));
    }

    public static void sendToDimension(ServerLevel dimension, Object packet) {
        CHANNEL.send(packet, PacketDistributor.DIMENSION.with(dimension.dimension()));
    }

    // Packet Classes

    public record KillNotificationPacket(
        String killerName,
        String killerUUIDString,
        String victimName,
        String victimUUIDString,
        float killerHealth,
        String weaponName,
        Double distance,
        long timestamp,
        boolean isHeadshot,
        boolean isSmash,
        boolean isCritical
    ) {
        public void encode(FriendlyByteBuf buf) {
            buf.writeUtf(killerName != null ? killerName : "");
            buf.writeUtf(killerUUIDString != null ? killerUUIDString : "");
            buf.writeUtf(victimName);
            buf.writeUtf(victimUUIDString != null ? victimUUIDString : "");
            buf.writeFloat(killerHealth);
            buf.writeUtf(weaponName);
            buf.writeDouble(distance != null ? distance : 0.0);
            buf.writeLong(timestamp);
            buf.writeBoolean(isHeadshot);
            buf.writeBoolean(isSmash);
            buf.writeBoolean(isCritical);
        }

        public static KillNotificationPacket decode(FriendlyByteBuf buf) {
            String killerName = buf.readUtf();
            if (killerName.isEmpty()) killerName = null;
            String killerUUID = buf.readUtf();
            if (killerUUID.isEmpty()) killerUUID = null;
            String victimName = buf.readUtf();
            String victimUUID = buf.readUtf();
            if (victimUUID.isEmpty()) victimUUID = null;
            float killerHealth = buf.readFloat();
            String weaponName = buf.readUtf();
            double d = buf.readDouble();
            Double distance = d == 0.0 ? null : d;
            long timestamp = buf.readLong();
            boolean isHeadshot = buf.readBoolean();
            boolean isSmash = buf.readBoolean();
            boolean isCritical = buf.readBoolean();

            return new KillNotificationPacket(
                killerName, killerUUID, victimName, victimUUID, killerHealth, weaponName, distance, timestamp, isHeadshot, isSmash, isCritical
            );
        }

        public void handleOnClient() {
            KillEntry entry = new KillEntry(
                killerName, killerUUIDString, victimName, victimUUIDString, killerHealth, weaponName, distance, timestamp, isHeadshot, isSmash, isCritical
            );
            com.eliasnvx.krylix.forge.client.KillFeedHud.addEntry(entry);
        }
    }

    public record DeathRecapPacket(
        String killerName,
        String killerUUIDString,
        float killerHealth,
        float killerMaxHealth,
        String weaponName,
        Double distance,
        float damageDealtToKiller,
        boolean isSmash,
        boolean isCritical,
        boolean isLongshot
    ) {
        public void encode(FriendlyByteBuf buf) {
            buf.writeUtf(killerName);
            buf.writeUtf(killerUUIDString != null ? killerUUIDString : "");
            buf.writeFloat(killerHealth);
            buf.writeFloat(killerMaxHealth);
            buf.writeUtf(weaponName);
            buf.writeDouble(distance != null ? distance : 0.0);
            buf.writeFloat(damageDealtToKiller);
            buf.writeBoolean(isSmash);
            buf.writeBoolean(isCritical);
            buf.writeBoolean(isLongshot);
        }

        public static DeathRecapPacket decode(FriendlyByteBuf buf) {
            String killerName = buf.readUtf();
            String killerUUID = buf.readUtf();
            if (killerUUID.isEmpty()) killerUUID = null;
            float killerHealth = buf.readFloat();
            float killerMaxHealth = buf.readFloat();
            String weaponName = buf.readUtf();
            double d = buf.readDouble();
            Double distance = d == 0.0 ? null : d;
            float damageDealt = buf.readFloat();
            boolean isSmash = buf.readBoolean();
            boolean isCritical = buf.readBoolean();
            boolean isLongshot = buf.readBoolean();

            return new DeathRecapPacket(killerName, killerUUID, killerHealth, killerMaxHealth, weaponName, distance, damageDealt, isSmash, isCritical, isLongshot);
        }

        public void handleOnClient() {
            com.eliasnvx.krylix.forge.client.DeathRecapClient.setRecap(this);
        }
    }

    public record MobStatsSyncPacket(Map<String, Integer> kills) {
        public void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(kills.size());
            for (Map.Entry<String, Integer> entry : kills.entrySet()) {
                buf.writeUtf(entry.getKey());
                buf.writeVarInt(entry.getValue());
            }
        }

        public static MobStatsSyncPacket decode(FriendlyByteBuf buf) {
            int size = buf.readVarInt();
            Map<String, Integer> kills = new HashMap<>();
            for (int i = 0; i < size; i++) {
                kills.put(buf.readUtf(), buf.readVarInt());
            }
            return new MobStatsSyncPacket(kills);
        }

        public void handleOnClient() {
            com.eliasnvx.krylix.forge.client.MobStatsClient.updateStats(kills);
        }
    }

    public record PlayerStatEntry(String uuid, String name, int kills, int deaths, int mobKills) {}

    public record PlayerStatsSyncPacket(List<PlayerStatEntry> entries) {
        public void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(entries.size());
            for (PlayerStatEntry entry : entries) {
                buf.writeUtf(entry.uuid());
                buf.writeUtf(entry.name());
                buf.writeVarInt(entry.kills());
                buf.writeVarInt(entry.deaths());
                buf.writeVarInt(entry.mobKills());
            }
        }

        public static PlayerStatsSyncPacket decode(FriendlyByteBuf buf) {
            int size = buf.readVarInt();
            List<PlayerStatEntry> entries = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                entries.add(new PlayerStatEntry(buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
            }
            return new PlayerStatsSyncPacket(entries);
        }

        public void handleOnClient() {
            com.eliasnvx.krylix.forge.client.PlayerStatsClient.updateStats(entries);
        }
    }
}
