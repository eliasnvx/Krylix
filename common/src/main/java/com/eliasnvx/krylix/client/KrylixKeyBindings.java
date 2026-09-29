package com.eliasnvx.krylix.client;

import com.eliasnvx.krylix.Krylix;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import java.util.List;

/** Krylix's keys, in their own "Krylix" section of the Controls screen. The loaders register {@link #ALL}. */
public final class KrylixKeyBindings {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Krylix.MOD_ID, "main"));

    public static final KeyMapping TOGGLE_KILL_FEED = new KeyMapping("key.krylix.toggle_killfeed", InputConstants.KEY_K, CATEGORY);
    public static final KeyMapping TOGGLE_MOB_STATS = new KeyMapping("key.krylix.toggle_mobstats", InputConstants.KEY_L, CATEGORY);
    public static final KeyMapping OPEN_LEADERBOARD = new KeyMapping("key.krylix.open_leaderboard", InputConstants.KEY_O, CATEGORY);
    public static final KeyMapping TOGGLE_HEALTH_PLATES = new KeyMapping("key.krylix.toggle_health_indicator", InputConstants.KEY_H, CATEGORY);

    public static final List<KeyMapping> ALL = List.of(TOGGLE_KILL_FEED, TOGGLE_MOB_STATS, OPEN_LEADERBOARD, TOGGLE_HEALTH_PLATES);

    private KrylixKeyBindings() {
    }
}
