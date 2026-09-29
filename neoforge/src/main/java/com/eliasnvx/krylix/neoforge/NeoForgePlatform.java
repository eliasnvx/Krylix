package com.eliasnvx.krylix.neoforge;

import com.eliasnvx.krylix.Krylix;
import com.eliasnvx.krylix.addon.AddonLoader;
import com.eliasnvx.krylix.api.KrylixAddon;
import com.eliasnvx.krylix.api.RegisterKrylixAddon;
import com.eliasnvx.krylix.platform.KrylixPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.List;

final class NeoForgePlatform implements KrylixPlatform {
    @Override
    public String loaderName() {
        return "neoforge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (player.connection.hasChannel(payload.type())) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    @Override
    public boolean canSendToServer(CustomPacketPayload.Type<?> type) {
        return FMLLoader.getCurrent().getDist().isClient() && Client.canSend(type);
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        Client.send(payload);
    }

    @Override
    public List<AddonLoader.DiscoveredAddon> discoverAddons() {
        List<AddonLoader.DiscoveredAddon> addons = new ArrayList<>();
        for (ModFileScanData scanData : ModList.get().getAllScanData()) {
            List<IModFileInfo> infos = scanData.getIModInfoData();
            String modId = infos.isEmpty() || infos.getFirst().getMods().isEmpty()
                ? "<unknown>"
                : infos.getFirst().getMods().getFirst().getModId();
            scanData.getAnnotatedBy(RegisterKrylixAddon.class, ElementType.TYPE)
                .map(data -> data.clazz().getClassName())
                .sorted()
                .forEach(className -> {
                    KrylixAddon addon = instantiate(modId, className);
                    if (addon != null) {
                        addons.add(new AddonLoader.DiscoveredAddon(modId, addon));
                    }
                });
        }
        return addons;
    }

    private static @Nullable KrylixAddon instantiate(String modId, String className) {
        try {
            Class<?> type = Class.forName(className, true, NeoForgePlatform.class.getClassLoader());
            if (!KrylixAddon.class.isAssignableFrom(type)) {
                Krylix.LOGGER.error("{} (mod {}) has @RegisterKrylixAddon but does not implement KrylixAddon", className, modId);
                return null;
            }
            return (KrylixAddon) type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | LinkageError e) {
            Krylix.LOGGER.error("Failed to create Krylix addon {} of mod {}", className, modId, e);
            return null;
        }
    }

    /** Client-only classes, loaded only when these are called on the client. */
    private static final class Client {
        static boolean canSend(CustomPacketPayload.Type<?> type) {
            var connection = Minecraft.getInstance().getConnection();
            return connection != null && connection.hasChannel(type);
        }

        static void send(CustomPacketPayload payload) {
            ClientPacketDistributor.sendToServer(payload);
        }
    }
}
