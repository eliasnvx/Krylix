package com.eliasnvx.krylix.platform;

import com.eliasnvx.krylix.addon.AddonLoader;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * The few things common code needs from the loader. Each loader installs its implementation first thing in its
 * mod initializer ({@link #install}); nothing in common runs before that.
 */
public interface KrylixPlatform {

    /** "fabric" or "neoforge". */
    String loaderName();

    boolean isModLoaded(String modId);

    /** Sends a Krylix payload if the player's client has Krylix; vanilla clients simply don't get it. */
    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    /** Client side: whether the server we are connected to accepts this Krylix payload (it has Krylix too). */
    boolean canSendToServer(CustomPacketPayload.Type<?> type);

    /** Client side: sends a payload to the server; check {@link #canSendToServer} first. */
    void sendToServer(CustomPacketPayload payload);

    /** Krylix addons of every loaded mod (Fabric entrypoint "krylix" / NeoForge {@code @RegisterKrylixAddon}). */
    List<AddonLoader.DiscoveredAddon> discoverAddons();

    static KrylixPlatform get() {
        KrylixPlatform platform = Holder.instance;
        if (platform == null) {
            throw new IllegalStateException("Krylix platform not installed yet");
        }
        return platform;
    }

    static void install(KrylixPlatform platform) {
        Holder.instance = platform;
    }

    final class Holder {
        private static volatile KrylixPlatform instance;

        private Holder() {
        }
    }
}
