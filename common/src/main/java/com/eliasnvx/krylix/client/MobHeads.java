package com.eliasnvx.krylix.client;

import com.eliasnvx.krylix.Krylix;
import com.eliasnvx.krylix.api.client.MobHead;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Mob faces for the feed, the mob panel and the recap. They come from resource packs
 * ({@code assets/<namespace>/krylix/heads/<path>.json}, Krylix ships the vanilla ones) and from addons
 * ({@code KrylixClientApi#registerMobHead}); a resource pack wins, so every head can be restyled without code.
 */
public final class MobHeads {
    public static final Identifier RELOAD_LISTENER_ID = Identifier.fromNamespaceAndPath(Krylix.MOD_ID, "mob_heads");

    private static volatile Map<Identifier, MobHead> fromResources = Map.of();
    private static final Map<Identifier, MobHead> fromAddons = new HashMap<>();
    /** Parsed entity type strings from payloads; the same few ids are asked for every frame. */
    private static final Map<String, Identifier> IDS = new HashMap<>();

    private MobHeads() {
    }

    public static @Nullable MobHead get(Identifier entityType) {
        MobHead head = fromResources.get(entityType);
        return head != null ? head : fromAddons.get(entityType);
    }

    public static @Nullable MobHead get(String entityType) {
        Identifier id = IDS.computeIfAbsent(entityType, key -> {
            Identifier parsed = Identifier.tryParse(key);
            return parsed != null ? parsed : Identifier.withDefaultNamespace("air");
        });
        return get(id);
    }

    static void register(Identifier entityType, MobHead head) {
        fromAddons.put(entityType, head);
    }

    public static void draw(GuiGraphicsExtractor graphics, MobHead head, int x, int y, int size, int color) {
        for (MobHead.Layer layer : head.layers()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, layer.texture(), x, y, layer.u(), layer.v(), size, size,
                layer.width(), layer.height(), layer.textureWidth(), layer.textureHeight(), color);
        }
    }

    /** Left a server: forget the entity ids it sent (a misbehaving server could send thousands). */
    public static void clearCache() {
        IDS.clear();
    }

    /** Reads every {@code krylix/heads} JSON on resource reload. Registered by each loader's client entrypoint. */
    public static final class Loader extends SimpleJsonResourceReloadListener<MobHead> {
        public Loader() {
            super(MobHead.CODEC, FileToIdConverter.json("krylix/heads"));
        }

        @Override
        protected void apply(Map<Identifier, MobHead> heads, ResourceManager manager, ProfilerFiller profiler) {
            fromResources = Map.copyOf(heads);
            Krylix.LOGGER.debug("Loaded {} Krylix mob heads", heads.size());
        }
    }
}
