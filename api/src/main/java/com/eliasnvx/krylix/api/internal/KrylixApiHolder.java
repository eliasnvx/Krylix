package com.eliasnvx.krylix.api.internal;

import com.eliasnvx.krylix.api.KrylixApi;
import com.eliasnvx.krylix.api.KrylixClientApi;
import org.jetbrains.annotations.ApiStatus;

/** Holds the API implementations Krylix installs. Not for addon use. */
@ApiStatus.Internal
public final class KrylixApiHolder {
    private static volatile KrylixApi common;
    private static volatile KrylixClientApi client;

    private KrylixApiHolder() {
    }

    public static KrylixApi common() {
        KrylixApi api = common;
        if (api == null) {
            throw new IllegalStateException("Krylix API used before Krylix initialized; use the KrylixApi passed to "
                + "KrylixAddon#onInitialize instead of calling KrylixApi.get() from a static initializer");
        }
        return api;
    }

    public static KrylixClientApi client() {
        KrylixClientApi api = client;
        if (api == null) {
            throw new IllegalStateException("Krylix client API used before Krylix initialized on the client (or on a server)");
        }
        return api;
    }

    public static synchronized void installCommon(KrylixApi api) {
        if (common != null) {
            throw new IllegalStateException("Krylix API is already installed");
        }
        common = api;
    }

    public static synchronized void installClient(KrylixClientApi api) {
        if (client != null) {
            throw new IllegalStateException("Krylix client API is already installed");
        }
        client = api;
    }
}
