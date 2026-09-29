package com.eliasnvx.krylix.api.client;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Supplies the numbers on the health plate for entities that don't keep their health in
 * {@code LivingEntity#getHealth()} (a multi-part boss, a mod with its own health system). Called on the client
 * thread, every frame the plate is shown: keep it cheap.
 */
@FunctionalInterface
public interface HealthProvider {
    /**
     * @param entity the entity under the crosshair
     * @return its health, or {@code null} to let the next provider (and finally vanilla) answer
     */
    @Nullable Health health(LivingEntity entity);

    /**
     * Health numbers for the plate.
     *
     * @param current current health, in half-hearts (vanilla's unit)
     * @param max     maximum health, in half-hearts, above zero
     */
    record Health(float current, float max) {
    }
}
