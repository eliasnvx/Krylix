package com.eliasnvx.krylix.network;

import com.eliasnvx.krylix.platform.KrylixPlatform;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Server-side sending. Players without Krylix are skipped by the platform, so vanilla clients can join. */
public final class KrylixNetwork {
    private KrylixNetwork() {
    }

    public static void toPlayer(ServerPlayer player, CustomPacketPayload payload) {
        KrylixPlatform.get().sendToPlayer(player, payload);
    }

    public static void toAll(MinecraftServer server, CustomPacketPayload payload) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            toPlayer(player, payload);
        }
    }

    public static void toDimension(ServerLevel level, CustomPacketPayload payload) {
        for (ServerPlayer player : level.players()) {
            toPlayer(player, payload);
        }
    }
}
