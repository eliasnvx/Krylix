package com.eliasnvx.krylix.neoforge;

import com.eliasnvx.krylix.Krylix;
import com.eliasnvx.krylix.config.KrylixConfig;
import com.eliasnvx.krylix.network.KrylixPayloads;
import com.eliasnvx.krylix.platform.KrylixPlatform;
import com.eliasnvx.krylix.server.KrylixServer;
import com.eliasnvx.krylix.server.KrylixServerCommands;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(Krylix.MOD_ID)
public final class KrylixNeoForge {
    public KrylixNeoForge(IEventBus modBus) {
        KrylixPlatform.install(new NeoForgePlatform());
        KrylixConfig.init();

        modBus.addListener(KrylixNeoForge::registerPayloads);
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> KrylixServerCommands.register(event.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((LivingDamageEvent.Post event) ->
            KrylixServer.onDamage(event.getEntity(), event.getSource(), event.getHealthDamage()));
        // Last, so a mod that cancels the death (a totem-like item) has had its say
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (LivingDeathEvent event) -> {
            if (!event.isCanceled()) {
                KrylixServer.onDeath(event.getEntity(), event.getSource());
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                KrylixServer.onJoin(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                KrylixServer.onLeave(player);
            }
        });
        Krylix.LOGGER.info("Krylix initialized on NeoForge");
    }

    /** Optional channels: vanilla clients can join a Krylix server and vice versa. */
    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Krylix.MOD_ID).versioned("1.4").optional().executesOn(HandlerThread.MAIN);
        for (KrylixPayloads.Entry<?> entry : KrylixPayloads.CLIENTBOUND) {
            registerClientbound(registrar, entry);
        }
        registrar.playToServer(KrylixPayloads.LeaderboardRequestPayload.TYPE, KrylixPayloads.LeaderboardRequestPayload.STREAM_CODEC,
            (payload, context) -> {
                if (context.player() instanceof ServerPlayer player) {
                    KrylixServer.onLeaderboardRequest(player);
                }
            });
    }

    private static <T extends CustomPacketPayload> void registerClientbound(PayloadRegistrar registrar, KrylixPayloads.Entry<T> entry) {
        registrar.playToClient(entry.type(), entry.codec(), KrylixNeoForge::handleOnClient);
    }

    /** Dedicated servers never get clientbound payloads; the client class is only touched on the client. */
    private static <T extends CustomPacketPayload> void handleOnClient(T payload, IPayloadContext context) {
        com.eliasnvx.krylix.client.KrylixClient.handle(payload);
    }
}
