package com.eliasnvx.krylix.api;

import com.eliasnvx.krylix.api.event.KrylixEventBus;
import com.eliasnvx.krylix.api.internal.KrylixApiHolder;
import com.eliasnvx.krylix.api.stats.KrylixStats;
import net.minecraft.server.MinecraftServer;

/**
 * The Krylix API. Addons get it in {@link KrylixAddon#onInitialize}; other code can use {@link #get()} once Krylix
 * has initialized.
 */
public interface KrylixApi {
    /** Krylix's mod id and resource namespace: {@value}. */
    String MOD_ID = "krylix";

    /**
     * @return the API
     * @throws IllegalStateException before Krylix has initialized
     */
    static KrylixApi get() {
        return KrylixApiHolder.common();
    }

    /**
     * The version of this API (SemVer), independent of the mod version. Minor versions only add things.
     *
     * @return for example {@code "1.0.0"}
     */
    String apiVersion();

    /**
     * The event bus. Server events are posted on the server thread, client events on the client thread.
     *
     * @return the event bus
     */
    KrylixEventBus events();

    /**
     * The all-time kill statistics stored in a server's world.
     *
     * @param server a running server (integrated or dedicated); server thread only
     * @return a read-only view
     */
    KrylixStats stats(MinecraftServer server);

    /**
     * Whether the kill feed is sent to players (switched by {@code /krylix toggle}).
     *
     * @return {@code true} while the feed is on
     */
    boolean isFeedEnabled();

    /**
     * Turns the kill feed on or off until the server stops, like {@code /krylix toggle}. Server thread only.
     *
     * @param enabled the new state
     */
    void setFeedEnabled(boolean enabled);
}
