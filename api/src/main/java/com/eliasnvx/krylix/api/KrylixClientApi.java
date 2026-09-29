package com.eliasnvx.krylix.api;

import com.eliasnvx.krylix.api.client.HealthProvider;
import com.eliasnvx.krylix.api.client.MobHead;
import com.eliasnvx.krylix.api.event.KrylixEventBus;
import com.eliasnvx.krylix.api.internal.KrylixApiHolder;
import com.eliasnvx.krylix.api.registry.ApiRegistry;
import net.minecraft.resources.Identifier;

import java.util.Optional;

/** The client half of the Krylix API. Client thread only. */
public interface KrylixClientApi {
    /**
     * @return the client API
     * @throws IllegalStateException before Krylix has initialized on the client
     */
    static KrylixClientApi get() {
        return KrylixApiHolder.client();
    }

    /**
     * The event bus, the same one as {@link KrylixApi#events()}; client events such as
     * {@link com.eliasnvx.krylix.api.event.client.FeedEntryEvent} are posted on it.
     *
     * @return the event bus
     */
    KrylixEventBus events();

    /**
     * Registers the face drawn for an entity type in the kill feed, the mob panel and the death recap.
     *
     * <p>A head from a resource pack ({@code assets/<namespace>/krylix/heads/<path>.json}) wins over one registered
     * here, so players and modpacks can always restyle it. For static textures, prefer shipping that JSON file in your
     * mod: no code needed.
     *
     * @param entityType the entity type id, for example {@code mymod:goblin}
     * @param head       the head
     * @throws IllegalStateException after client initialization
     */
    void registerMobHead(Identifier entityType, MobHead head);

    /**
     * The head drawn for an entity type: from resource packs, then from {@link #registerMobHead}.
     *
     * @param entityType the entity type id
     * @return the head, or empty when Krylix draws a plain placeholder
     */
    Optional<MobHead> mobHead(Identifier entityType);

    /**
     * Health providers for the health plate over the targeted entity, for mods whose entities keep their health
     * somewhere else than {@code LivingEntity#getHealth()}. They are asked in registration order; the first non-null
     * answer wins, and vanilla health is used when none answers.
     *
     * @return the registry, frozen after client initialization
     */
    ApiRegistry<HealthProvider> healthProviders();
}
