package com.eliasnvx.krylix.fabric;

import com.eliasnvx.krylix.Krylix;
import com.eliasnvx.krylix.addon.KrylixApiImpl;
import com.eliasnvx.krylix.config.KrylixConfig;
import com.eliasnvx.krylix.network.KrylixPayloads;
import com.eliasnvx.krylix.platform.KrylixPlatform;
import com.eliasnvx.krylix.server.KrylixServer;
import com.eliasnvx.krylix.server.KrylixServerCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class KrylixFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        KrylixPlatform.install(new FabricPlatform());
        KrylixConfig.init();

        for (KrylixPayloads.Entry<?> entry : KrylixPayloads.CLIENTBOUND) {
            registerClientbound(entry);
        }
        for (KrylixPayloads.Entry<?> entry : KrylixPayloads.SERVERBOUND) {
            registerServerbound(entry);
        }
        ServerPlayNetworking.registerGlobalReceiver(KrylixPayloads.LeaderboardRequestPayload.TYPE,
            (payload, context) -> KrylixServer.onLeaderboardRequest(context.player()));

        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> KrylixServerCommands.register(dispatcher));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> KrylixServer.onJoin(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> KrylixServer.onLeave(handler.getPlayer()));
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (!blocked) {
                KrylixServer.onDamage(entity, source, damageTaken);
            }
        });
        ServerLivingEntityEvents.AFTER_DEATH.register(KrylixServer::onDeath);

        KrylixApiImpl.init(); // last: addons see a fully set up Krylix
        Krylix.LOGGER.info("Krylix initialized on Fabric");
    }

    private static <T extends CustomPacketPayload> void registerClientbound(KrylixPayloads.Entry<T> entry) {
        PayloadTypeRegistry.clientboundPlay().register(entry.type(), entry.codec());
    }

    private static <T extends CustomPacketPayload> void registerServerbound(KrylixPayloads.Entry<T> entry) {
        PayloadTypeRegistry.serverboundPlay().register(entry.type(), entry.codec());
    }
}
