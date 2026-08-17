package com.eliasnvx.krylix.forge.client;

import com.eliasnvx.krylix.core.HealthBarStyle;
import com.eliasnvx.krylix.forge.config.KrylixConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;

import java.util.List;

public class HealthIndicator {
    private static final double MAX_DISTANCE = 48.0;
    private static final int BAR_SEGMENTS = 10;

    private static LivingEntity currentTarget = null;

    public static void setEnabled(boolean enabled) {
        KrylixConfig.get().healthIndicatorEnabled = enabled;
        KrylixConfig.save();
    }

    public static boolean isIndicatorEnabled() {
        return KrylixConfig.get().healthIndicatorEnabled;
    }

    public static LivingEntity getCurrentTarget() {
        return currentTarget;
    }

    public static void updateTarget() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            currentTarget = null;
            return;
        }

        Entity looked = getEntityLookedAt(mc.player);
        if (looked instanceof LivingEntity living && living.isAlive()) {
            currentTarget = living;
        } else if (mc.crosshairPickEntity instanceof LivingEntity le && le.isAlive()) {
            currentTarget = le;
        } else {
            currentTarget = null;
        }
    }

    private static Entity getEntityLookedAt(Entity e) {
        Entity foundEntity = null;
        final double finalDistance = MAX_DISTANCE;
        HitResult pos = raycast(e, finalDistance);
        Vec3 positionVector = e.getEyePosition();

        double distance = pos.getLocation().distanceTo(positionVector);

        Vec3 lookVector = e.getLookAngle();
        Vec3 reachVector = positionVector.add(lookVector.x * finalDistance, lookVector.y * finalDistance, lookVector.z * finalDistance);

        AABB searchBox = e.getBoundingBox().inflate(lookVector.x * finalDistance, lookVector.y * finalDistance, lookVector.z * finalDistance).inflate(1.0);
        List<Entity> entitiesInBoundingBox = e.level().getEntities(e, searchBox);
        double minDistance = distance;

        for (Entity entity : entitiesInBoundingBox) {
            Entity lookedEntity = null;
            if (entity.isPickable()) {
                AABB collisionBox = entity.getBoundingBox().inflate(0.5);
                java.util.Optional<Vec3> interceptPosition = collisionBox.clip(positionVector, reachVector);

                if (collisionBox.contains(positionVector)) {
                    if (0.0D < minDistance || minDistance == 0.0D) {
                        lookedEntity = entity;
                        minDistance = 0.0D;
                    }
                } else if (interceptPosition.isPresent()) {
                    double distanceToEntity = positionVector.distanceTo(interceptPosition.get());

                    if (distanceToEntity < minDistance || minDistance == 0.0D) {
                        lookedEntity = entity;
                        minDistance = distanceToEntity;
                    }
                }
            }

            if (lookedEntity != null && minDistance < distance) {
                foundEntity = lookedEntity;
            }
        }

        return foundEntity;
    }

    private static HitResult raycast(Entity e, double len) {
        Vec3 origin = e.getEyePosition();
        Vec3 ray = e.getLookAngle();
        Vec3 next = origin.add(ray.normalize().scale(len));
        return e.level().clip(new ClipContext(origin, next, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, e));
    }

    public static Component createHealthComponent(LivingEntity living) {
        float maxHealth = Math.max(1f, living.getMaxHealth());
        float pct = Math.max(0f, Math.min(1f, living.getHealth() / maxHealth));
        int hp = Math.round(living.getHealth());
        int maxHp = Math.round(maxHealth);

        int color;
        if (pct > 0.6f) color = 0x55FF55;
        else if (pct > 0.3f) color = 0xFFFF55;
        else color = 0xFF5555;

        String bar = barText(pct, hp, maxHp);
        return Component.literal(bar).withStyle(style -> style.withColor(color));
    }

    public static void onRenderNameTag(RenderNameTagEvent.CanRender event) {
        if (!isIndicatorEnabled()) return;
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || entity == mc.player) return;

        updateTarget();
        if (currentTarget == living) {
            EntityRenderState state = event.getEntityRenderState();
            if (state.nameTag == null) {
                state.nameTag = living.getDisplayName();
            }
            if (state.nameTagAttachment == null) {
                state.nameTagAttachment = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getYRot(event.getPartialTick()));
                if (state.nameTagAttachment == null) {
                    state.nameTagAttachment = new Vec3(0, entity.getBbHeight() + 0.5, 0);
                }
            }
            state.scoreText = createHealthComponent(living);
            event.setCanRender(net.minecraft.util.TriState.TRUE);
        }
    }

    private static String barText(float pct, int hp, int maxHp) {
        int filled = Math.max(0, Math.min(BAR_SEGMENTS, Math.round(pct * BAR_SEGMENTS)));
        int empty = BAR_SEGMENTS - filled;
        
        HealthBarStyle style = KrylixConfig.get().healthBarStyle;
        if (style == null) style = HealthBarStyle.BLOCKS;

        return switch (style) {
            case BLOCKS -> "█".repeat(filled) + "░".repeat(empty) + " " + hp + "/" + maxHp;
            case ASCII -> "[" + "|".repeat(filled) + ".".repeat(empty) + "] " + hp + "/" + maxHp;
            case DOTS -> "●".repeat(filled) + "○".repeat(empty) + " " + hp + "/" + maxHp;
            case NUMBER_ONLY -> hp + "/" + maxHp + " HP";
        };
    }
}
