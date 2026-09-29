package com.eliasnvx.krylix.client;

import com.eliasnvx.krylix.Krylix;
import com.eliasnvx.krylix.addon.AddonLoader;
import com.eliasnvx.krylix.addon.KrylixApiImpl;
import com.eliasnvx.krylix.addon.SimpleApiRegistry;
import com.eliasnvx.krylix.api.KrylixClientApi;
import com.eliasnvx.krylix.api.client.HealthProvider;
import com.eliasnvx.krylix.api.client.MobHead;
import com.eliasnvx.krylix.api.event.KrylixEventBus;
import com.eliasnvx.krylix.api.internal.KrylixApiHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public final class KrylixClientApiImpl implements KrylixClientApi {
    private static final KrylixClientApiImpl INSTANCE = new KrylixClientApiImpl();
    private static final SimpleApiRegistry<HealthProvider> HEALTH_PROVIDERS =
        new SimpleApiRegistry<>(Identifier.fromNamespaceAndPath(Krylix.MOD_ID, "health_provider"));
    private static boolean frozen;

    private KrylixClientApiImpl() {
    }

    /** Installs the client API and runs the addons' client hooks. After {@code KrylixApiImpl.init()}. */
    public static void init() {
        KrylixApiHolder.installClient(INSTANCE);
        AddonLoader.initClient(INSTANCE);
        HEALTH_PROVIDERS.freeze();
        frozen = true;
        Krylix.LOGGER.info("Krylix client API ready: {} addon(s), {} health provider(s)",
            AddonLoader.addons().size(), HEALTH_PROVIDERS.values().size());
    }

    @Override
    public KrylixEventBus events() {
        return KrylixApiImpl.EVENTS;
    }

    @Override
    public void registerMobHead(Identifier entityType, MobHead head) {
        if (frozen) {
            throw new IllegalStateException("Mob heads are registered during KrylixAddon#onInitializeClient");
        }
        MobHeads.register(entityType, head);
    }

    @Override
    public Optional<MobHead> mobHead(Identifier entityType) {
        return Optional.ofNullable(MobHeads.get(entityType));
    }

    @Override
    public SimpleApiRegistry<HealthProvider> healthProviders() {
        return HEALTH_PROVIDERS;
    }

    /** The first addon answer, else vanilla health. */
    public static HealthProvider.Health healthOf(LivingEntity entity) {
        for (HealthProvider provider : HEALTH_PROVIDERS.values()) {
            try {
                HealthProvider.Health health = provider.health(entity);
                if (health != null && health.max() > 0) {
                    return health;
                }
            } catch (Throwable t) {
                Krylix.LOGGER.error("A Krylix health provider failed for {}", entity, t);
            }
        }
        return new HealthProvider.Health(entity.getHealth(), entity.getMaxHealth());
    }
}
