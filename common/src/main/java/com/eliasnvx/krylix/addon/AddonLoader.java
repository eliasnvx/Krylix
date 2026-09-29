package com.eliasnvx.krylix.addon;

import com.eliasnvx.krylix.Krylix;
import com.eliasnvx.krylix.api.KrylixAddon;
import com.eliasnvx.krylix.api.KrylixApi;
import com.eliasnvx.krylix.api.KrylixClientApi;
import com.eliasnvx.krylix.platform.KrylixPlatform;

import java.util.Comparator;
import java.util.List;

/** Finds addons through the loader and runs their init hooks; one failing addon never stops the others. */
public final class AddonLoader {
    private static List<DiscoveredAddon> addons = List.of();

    private AddonLoader() {
    }

    /** Called by each loader once Krylix itself is set up (both sides). */
    public static void initCommon(KrylixApi api) {
        addons = KrylixPlatform.get().discoverAddons().stream()
            .sorted(Comparator.comparing(DiscoveredAddon::modId).thenComparing(a -> a.addon().getClass().getName()))
            .toList();
        for (DiscoveredAddon addon : addons) {
            try {
                addon.addon().onInitialize(api);
                Krylix.LOGGER.info("Loaded Krylix addon {} ({})", addon.modId(), addon.addon().getClass().getName());
            } catch (Throwable t) {
                Krylix.LOGGER.error("Krylix addon {} failed in onInitialize", addon.modId(), t);
            }
        }
    }

    /** Called by each loader on the physical client, after {@link #initCommon}. */
    public static void initClient(KrylixClientApi api) {
        for (DiscoveredAddon addon : addons) {
            try {
                addon.addon().onInitializeClient(api);
            } catch (Throwable t) {
                Krylix.LOGGER.error("Krylix addon {} failed in onInitializeClient", addon.modId(), t);
            }
        }
    }

    public static List<DiscoveredAddon> addons() {
        return addons;
    }

    /** An addon and the mod that ships it. */
    public record DiscoveredAddon(String modId, KrylixAddon addon) {
    }
}
