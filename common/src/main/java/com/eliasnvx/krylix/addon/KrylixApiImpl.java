package com.eliasnvx.krylix.addon;

import com.eliasnvx.krylix.api.KrylixApi;
import com.eliasnvx.krylix.api.internal.KrylixApiHolder;
import com.eliasnvx.krylix.api.stats.KrylixStats;
import com.eliasnvx.krylix.server.KrylixServer;
import com.eliasnvx.krylix.server.PlayerKillStatsData;
import net.minecraft.server.MinecraftServer;

public final class KrylixApiImpl implements KrylixApi {
    /** Bumped with the api module: minor for additions, major for breaking changes. */
    public static final String API_VERSION = "1.0.0";
    public static final SimpleEventBus EVENTS = new SimpleEventBus();

    private static final KrylixApiImpl INSTANCE = new KrylixApiImpl();

    private KrylixApiImpl() {
    }

    /** Installs the API and runs the addons. Each loader calls this once, after Krylix is set up. */
    public static void init() {
        KrylixApiHolder.installCommon(INSTANCE);
        AddonLoader.initCommon(INSTANCE);
    }

    @Override
    public String apiVersion() {
        return API_VERSION;
    }

    @Override
    public SimpleEventBus events() {
        return EVENTS;
    }

    @Override
    public KrylixStats stats(MinecraftServer server) {
        return PlayerKillStatsData.get(server).view();
    }

    @Override
    public boolean isFeedEnabled() {
        return KrylixServer.isFeedEnabled();
    }

    @Override
    public void setFeedEnabled(boolean enabled) {
        KrylixServer.setFeedEnabled(enabled);
    }
}
