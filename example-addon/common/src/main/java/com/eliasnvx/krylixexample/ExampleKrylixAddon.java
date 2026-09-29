package com.eliasnvx.krylixexample;

import com.eliasnvx.krylix.api.KrylixAddon;
import com.eliasnvx.krylix.api.KrylixApi;
import com.eliasnvx.krylix.api.KrylixClientApi;
import com.eliasnvx.krylix.api.RegisterKrylixAddon;
import com.eliasnvx.krylix.api.client.HealthProvider;
import com.eliasnvx.krylix.api.client.MobHead;
import com.eliasnvx.krylix.api.event.KillCreditEvent;
import com.eliasnvx.krylix.api.event.KillEvent;
import com.eliasnvx.krylix.api.event.StatRecordedEvent;
import com.eliasnvx.krylix.api.event.client.FeedEntryEvent;
import com.eliasnvx.krylix.api.stats.StatType;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

/**
 * A small Krylix addon that uses every part of the API. Fabric finds it through the "krylix" entrypoint in
 * fabric.mod.json, NeoForge through {@link RegisterKrylixAddon}.
 */
@RegisterKrylixAddon
public final class ExampleKrylixAddon implements KrylixAddon {
    public static final String MOD_ID = "krylix_example";
    /** {@code /tag @s add krylix_example.practice}: your deaths and kills are not counted or announced. */
    public static final String PRACTICE_TAG = "krylix_example.practice";

    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize(KrylixApi api) {
        LOGGER.info("Krylix example addon on API {}", api.apiVersion());

        // Friendly fire: killing a teammate is no kill (the feed shows it like a fall, the kill is not counted)
        api.events().addListener(KillCreditEvent.class, event -> {
            if (event.killer() != null && event.killer().isAlliedTo(event.victim())) {
                event.setKiller(null);
            }
        });

        // Practice mode: tagged players' fights stay out of the feed and the statistics
        api.events().addListener(KillEvent.class, event -> {
            boolean practice = event.victim().entityTags().contains(PRACTICE_TAG)
                || event.killer() != null && event.killer().entityTags().contains(PRACTICE_TAG);
            if (practice) {
                event.setBroadcast(false);
                event.setRecordStats(false);
            }
        });

        // Milestones: a hook for rewards, Discord webhooks, external leaderboards...
        api.events().addListener(StatRecordedEvent.class, event -> {
            if (event.type() == StatType.KILL && event.total() % 10 == 0) {
                LOGGER.info("{} reached {} player kills", event.playerName(), event.total());
            }
        });
    }

    @Override
    public void onInitializeClient(KrylixClientApi api) {
        // A head from code. Static textures are simpler as JSON: see assets/minecraft/krylix/heads/zombie_horse.json
        api.registerMobHead(Identifier.withDefaultNamespace("giant"),
            MobHead.of(Identifier.withDefaultNamespace("textures/entity/zombie/zombie.png"), 8, 8, 8, 8, 64, 64));

        // Absorption hearts (golden apples, totems) count on the health plate
        api.healthProviders().register(Identifier.fromNamespaceAndPath(MOD_ID, "absorption"), entity -> {
            float absorption = entity.getAbsorptionAmount();
            return absorption > 0
                ? new HealthProvider.Health(entity.getHealth() + absorption, entity.getMaxHealth() + absorption)
                : null;
        });

        // A quieter feed: no rows for plain falls
        api.events().addListener(FeedEntryEvent.class, event -> {
            if (event.killer() == null && event.weapon().equals(Identifier.withDefaultNamespace("feather"))) {
                event.cancel();
            }
        });
    }
}
