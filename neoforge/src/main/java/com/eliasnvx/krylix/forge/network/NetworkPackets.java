package com.eliasnvx.krylix.forge.network;

import com.eliasnvx.krylix.Krylix;
import com.eliasnvx.krylix.model.KillEntry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkPackets {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Krylix.MOD_ID).versioned("1");

        registrar.playToClient(
            KillNotificationPacket.TYPE,
            KillNotificationPacket.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(payload::handleOnClient)
        );

        registrar.playToClient(
            MobStatsSyncPacket.TYPE,
            MobStatsSyncPacket.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(payload::handleOnClient)
        );

        registrar.playToClient(
            PlayerStatsSyncPacket.TYPE,
            PlayerStatsSyncPacket.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(payload::handleOnClient)
        );

        registrar.playToClient(
            DeathRecapPacket.TYPE,
            DeathRecapPacket.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(payload::handleOnClient)
        );
    }

    public static void sendToAll(CustomPacketPayload packet) {
        PacketDistributor.sendToAllPlayers(packet);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToDimension(ServerLevel dimension, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayersInDimension(dimension, packet);
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
    ) implements CustomPacketPayload {
        public static final Type<KillNotificationPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Krylix.MOD_ID, "kill_notification"));

        public static final StreamCodec<FriendlyByteBuf, KillNotificationPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeUtf(packet.killerName() != null ? packet.killerName() : "");
                buf.writeUtf(packet.killerUUIDString() != null ? packet.killerUUIDString() : "");
                buf.writeUtf(packet.victimName());
                buf.writeUtf(packet.victimUUIDString() != null ? packet.victimUUIDString() : "");
                buf.writeFloat(packet.killerHealth());
                buf.writeUtf(packet.weaponName());
                buf.writeDouble(packet.distance() != null ? packet.distance() : 0.0);
                buf.writeLong(packet.timestamp());
                buf.writeBoolean(packet.isHeadshot());
                buf.writeBoolean(packet.isSmash());
                buf.writeBoolean(packet.isCritical());
            },
            buf -> {
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
        );

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }

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
    ) implements CustomPacketPayload {
        public static final Type<DeathRecapPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Krylix.MOD_ID, "death_recap"));

        public static final StreamCodec<FriendlyByteBuf, DeathRecapPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeUtf(packet.killerName());
                buf.writeUtf(packet.killerUUIDString() != null ? packet.killerUUIDString() : "");
                buf.writeFloat(packet.killerHealth());
                buf.writeFloat(packet.killerMaxHealth());
                buf.writeUtf(packet.weaponName());
                buf.writeDouble(packet.distance() != null ? packet.distance() : 0.0);
                buf.writeFloat(packet.damageDealtToKiller());
                buf.writeBoolean(packet.isSmash());
                buf.writeBoolean(packet.isCritical());
                buf.writeBoolean(packet.isLongshot());
            },
            buf -> {
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
        );

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public void handleOnClient() {
            com.eliasnvx.krylix.forge.client.DeathRecapClient.setRecap(this);
        }
    }

    public record MobStatsSyncPacket(Map<String, Integer> kills) implements CustomPacketPayload {
        public static final Type<MobStatsSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Krylix.MOD_ID, "mob_stats_sync"));

        public static final StreamCodec<FriendlyByteBuf, MobStatsSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.kills().size());
                for (Map.Entry<String, Integer> entry : packet.kills().entrySet()) {
                    buf.writeUtf(entry.getKey());
                    buf.writeVarInt(entry.getValue());
                }
            },
            buf -> {
                int size = buf.readVarInt();
                Map<String, Integer> kills = new HashMap<>();
                for (int i = 0; i < size; i++) {
                    kills.put(buf.readUtf(), buf.readVarInt());
                }
                return new MobStatsSyncPacket(kills);
            }
        );

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public void handleOnClient() {
            com.eliasnvx.krylix.forge.client.MobStatsClient.updateStats(kills);
        }
    }

    public record PlayerStatEntry(String uuid, String name, int kills, int deaths, int mobKills) {}

    public record PlayerStatsSyncPacket(List<PlayerStatEntry> entries) implements CustomPacketPayload {
        public static final Type<PlayerStatsSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Krylix.MOD_ID, "player_stats_sync"));

        public static final StreamCodec<FriendlyByteBuf, PlayerStatsSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.entries().size());
                for (PlayerStatEntry entry : packet.entries()) {
                    buf.writeUtf(entry.uuid());
                    buf.writeUtf(entry.name());
                    buf.writeVarInt(entry.kills());
                    buf.writeVarInt(entry.deaths());
                    buf.writeVarInt(entry.mobKills());
                }
            },
            buf -> {
                int size = buf.readVarInt();
                List<PlayerStatEntry> entries = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    entries.add(new PlayerStatEntry(buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
                }
                return new PlayerStatsSyncPacket(entries);
            }
        );

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public void handleOnClient() {
            com.eliasnvx.krylix.forge.client.PlayerStatsClient.updateStats(entries);
        }
    }
}
