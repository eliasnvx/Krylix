package com.eliasnvx.krylix.fabric;

import com.eliasnvx.krylix.Krylix;
import com.eliasnvx.krylix.addon.AddonLoader;
import com.eliasnvx.krylix.api.KrylixAddon;
import com.eliasnvx.krylix.platform.KrylixPlatform;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

final class FabricPlatform implements KrylixPlatform {
    @Override
    public String loaderName() {
        return "fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (ServerPlayNetworking.canSend(player, payload.type())) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    @Override
    public boolean canSendToServer(CustomPacketPayload.Type<?> type) {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT && Client.canSend(type);
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        Client.send(payload);
    }

    @Override
    public List<AddonLoader.DiscoveredAddon> discoverAddons() {
        List<AddonLoader.DiscoveredAddon> addons = new ArrayList<>();
        for (EntrypointContainer<KrylixAddon> container
            : FabricLoader.getInstance().getEntrypointContainers(KrylixAddon.FABRIC_ENTRYPOINT, KrylixAddon.class)) {
            String modId = container.getProvider().getMetadata().getId();
            try {
                addons.add(new AddonLoader.DiscoveredAddon(modId, container.getEntrypoint()));
            } catch (Throwable t) {
                Krylix.LOGGER.error("Failed to create the Krylix addon of mod {}", modId, t);
            }
        }
        return addons;
    }

    /** Client-only classes, loaded only when these are called on the client. */
    private static final class Client {
        static boolean canSend(CustomPacketPayload.Type<?> type) {
            return ClientPlayNetworking.canSend(type);
        }

        static void send(CustomPacketPayload payload) {
            ClientPlayNetworking.send(payload);
        }
    }
}
