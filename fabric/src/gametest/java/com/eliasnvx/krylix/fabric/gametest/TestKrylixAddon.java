package com.eliasnvx.krylix.fabric.gametest;

import com.eliasnvx.krylix.api.KrylixAddon;
import com.eliasnvx.krylix.api.KrylixApi;
import com.eliasnvx.krylix.api.KrylixClientApi;
import com.eliasnvx.krylix.api.client.HealthProvider;
import com.eliasnvx.krylix.api.client.MobHead;
import com.eliasnvx.krylix.api.event.EventPriority;
import com.eliasnvx.krylix.api.event.KillCreditEvent;
import com.eliasnvx.krylix.api.event.KillEvent;
import com.eliasnvx.krylix.api.event.StatRecordedEvent;
import com.eliasnvx.krylix.api.event.client.FeedEntryEvent;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/** A test addon, discovered through the "krylix" entrypoint; {@link AddonApiClientTest} checks what it saw. */
public final class TestKrylixAddon implements KrylixAddon {
    /** Mobs with this tag die without Krylix noticing (the KillEvent is cancelled). */
    static final String IGNORED_TAG = "krylix_test.ignored";
    /** Entities with this custom name get their health from the test provider. */
    static final String HEALTH_PROBE = "HealthProbe";
    static final Identifier OVERRIDDEN_ZOMBIE_TEXTURE = Identifier.fromNamespaceAndPath("krylix_test", "textures/not_used.png");

    static volatile boolean initialized;
    static volatile boolean clientInitialized;
    static final AtomicInteger credits = new AtomicInteger();
    static final List<String> kills = new CopyOnWriteArrayList<>();
    static final List<StatRecordedEvent> stats = new CopyOnWriteArrayList<>();
    static final List<FeedEntryEvent> feedRows = new CopyOnWriteArrayList<>();

    @Override
    public void onInitialize(KrylixApi api) {
        initialized = true;
        api.events().addListener(KillCreditEvent.class, event -> credits.incrementAndGet());
        api.events().addListener(KillEvent.class, EventPriority.HIGH, event -> {
            if (event.victim().entityTags().contains(IGNORED_TAG)) {
                event.cancel();
            }
        });
        api.events().addListener(KillEvent.class, event ->
            kills.add(net.minecraft.world.entity.EntityType.getKey(event.victim().getType()).toString()));
        api.events().addListener(StatRecordedEvent.class, stats::add);
    }

    @Override
    public void onInitializeClient(KrylixClientApi api) {
        clientInitialized = true;
        api.registerMobHead(Identifier.withDefaultNamespace("giant"),
            MobHead.of(Identifier.withDefaultNamespace("textures/entity/zombie/zombie.png"), 8, 8, 8, 8, 64, 64));
        // Krylix ships a zombie head in its resources: that one must win over this
        api.registerMobHead(Identifier.withDefaultNamespace("zombie"), MobHead.of(OVERRIDDEN_ZOMBIE_TEXTURE, 0, 0, 8, 8, 64, 64));
        api.healthProviders().register(Identifier.fromNamespaceAndPath("krylix_test", "probe"), entity ->
            entity.hasCustomName() && HEALTH_PROBE.equals(entity.getCustomName().getString())
                ? new HealthProvider.Health(7, 9)
                : null);
        api.events().addListener(FeedEntryEvent.class, feedRows::add);
    }
}
