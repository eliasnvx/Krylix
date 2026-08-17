package com.eliasnvx.krylix.forge.client;

import com.eliasnvx.krylix.forge.config.KrylixConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
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

    private static final Identifier SPRITE_HEART_FULL = Identifier.withDefaultNamespace("hud/heart/full");
    private static final Identifier SPRITE_HEART_HALF = Identifier.withDefaultNamespace("hud/heart/half");
    private static final Identifier SPRITE_HEART_CONTAINER = Identifier.withDefaultNamespace("hud/heart/container");

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

    public static void onCanRenderNameTag(RenderNameTagEvent.CanRender event) {
        if (!isIndicatorEnabled()) return;
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || entity == mc.player) return;

        updateTarget();
        if (currentTarget == living) {
            EntityRenderState state = event.getEntityRenderState();
            if (event.getContent() == null) {
                event.setContent(living.getDisplayName());
            }
            Vec3 attach = state.nameTagAttachment;
            if (attach == null) {
                attach = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getYRot(event.getPartialTick()));
                if (attach == null) {
                    attach = new Vec3(0, entity.getBbHeight() + 0.5, 0);
                }
            }
            // Lift the entire panel +0.75 blocks above the mob
            state.nameTagAttachment = attach.add(0, 0.75, 0);
            event.setCanRender(net.minecraft.util.TriState.TRUE);
        }
    }

    public static void onDoRenderNameTag(RenderNameTagEvent.DoRender event) {
        if (!isIndicatorEnabled()) return;
        LivingEntity living = currentTarget;
        if (living == null || !living.isAlive()) return;
        EntityRenderState state = event.getEntityRenderState();
        if (state.nameTagAttachment == null) return;

        renderTexturedHealth(state, event.getPoseStack(), event.getSubmitNodeCollector(), event.getCameraRenderState(), living);
    }

    public static void renderTexturedHealth(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, LivingEntity living) {
        float maxHp = Math.max(1f, living.getMaxHealth());
        float hp = Math.max(0f, Math.min(maxHp, living.getHealth()));
        int hpRounded = Math.round(hp);
        int maxHpRounded = Math.round(maxHp);

        int totalHearts = Math.min(10, Math.max(1, (int) Math.ceil(maxHp / 2.0)));
        float hpPerHeart = maxHp / (float) totalHearts;
        int fullHearts = (int) (hp / hpPerHeart);
        boolean hasHalf = (hp - (fullHearts * hpPerHeart)) >= (hpPerHeart * 0.25f);
        int emptyHearts = totalHearts - fullHearts - (hasHalf ? 1 : 0);

        MutableComponent healthLine = Component.empty();
        for (int i = 0; i < fullHearts; i++) {
            healthLine.append(Component.object(new AtlasSprite(AtlasIds.GUI, SPRITE_HEART_FULL)));
        }
        if (hasHalf) {
            healthLine.append(Component.object(new AtlasSprite(AtlasIds.GUI, SPRITE_HEART_HALF)));
        }
        for (int i = 0; i < emptyHearts; i++) {
            healthLine.append(Component.object(new AtlasSprite(AtlasIds.GUI, SPRITE_HEART_CONTAINER)));
        }
        healthLine.append(Component.literal(" " + hpRounded + "/" + maxHpRounded).withStyle(ChatFormatting.WHITE));

        submitNodeCollector.submitNameTag(poseStack, state.nameTagAttachment, 10, healthLine, state.isDiscrete, state.lightCoords, state.distanceToCameraSq, cameraRenderState);
    }
}
