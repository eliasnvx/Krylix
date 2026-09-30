package com.eliasnvx.krylix.client;

import com.eliasnvx.krylix.Krylix;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Krylix's keys, in their own "Krylix" section of the Controls screen.
 *
 * <p>The category is made by each loader, the way that loader expects: Fabric through vanilla's
 * {@code KeyMapping.Category.register} (Fabric API sorts modded categories there), NeoForge with the plain constructor
 * and {@code RegisterKeyMappingsEvent#registerCategory} (NeoForge deprecates {@code register}, which would add the
 * category to vanilla's sort order a second time). The loader then registers the keys {@link #create} returns.
 */
public final class KrylixKeyBindings {
    public static final Identifier CATEGORY_ID = Identifier.fromNamespaceAndPath(Krylix.MOD_ID, "main");

    private static @Nullable KeyMapping toggleKillFeed;
    private static @Nullable KeyMapping toggleMobStats;
    private static @Nullable KeyMapping openLeaderboard;
    private static @Nullable KeyMapping toggleHealthPlates;

    private KrylixKeyBindings() {
    }

    /** Creates the keys in {@code category}; called once by the loader, before the options load. */
    public static List<KeyMapping> create(KeyMapping.Category category) {
        if (toggleKillFeed != null) {
            throw new IllegalStateException("Krylix key mappings already created");
        }
        toggleKillFeed = new KeyMapping("key.krylix.toggle_killfeed", InputConstants.KEY_K, category);
        // Not L or O: in 26.3 vanilla binds L to Advancements and O to the Friends overlay (O never reaches mods)
        toggleMobStats = new KeyMapping("key.krylix.toggle_mobstats", InputConstants.KEY_J, category);
        openLeaderboard = new KeyMapping("key.krylix.open_leaderboard", InputConstants.KEY_U, category);
        toggleHealthPlates = new KeyMapping("key.krylix.toggle_health_indicator", InputConstants.KEY_H, category);
        return List.of(toggleKillFeed, toggleMobStats, openLeaderboard, toggleHealthPlates);
    }

    /** Whether the loader has created the keys yet (it does so before the first client tick). */
    static boolean ready() {
        return toggleKillFeed != null;
    }

    static KeyMapping toggleKillFeed() {
        return toggleKillFeed;
    }

    static KeyMapping toggleMobStats() {
        return toggleMobStats;
    }

    static KeyMapping openLeaderboard() {
        return openLeaderboard;
    }

    static KeyMapping toggleHealthPlates() {
        return toggleHealthPlates;
    }
}
