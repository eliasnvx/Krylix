package com.eliasnvx.krylix.client;

import com.eliasnvx.krylix.config.KrylixConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;

import java.util.List;

/** The health plate over the entity under the crosshair. Client thread only. */
public final class HealthIndicator {
    private static final double MAX_DISTANCE = 48.0;

    private static final Identifier SPRITE_HEART_FULL = Identifier.withDefaultNamespace("hud/heart/full");
    private static final Identifier SPRITE_HEART_HALF = Identifier.withDefaultNamespace("hud/heart/half");
    private static final Identifier SPRITE_HEART_CONTAINER = Identifier.withDefaultNamespace("hud/heart/container");

    private static LivingEntity currentTarget = null;
    /** Render state extracted for {@link #currentTarget} this frame: only that nametag gets the health plate. */
    private static EntityRenderState targetState = null;

    public static void setEnabled(boolean enabled) {
        KrylixConfig.get().healthIndicatorEnabled = enabled;
        KrylixConfig.save();
    }

    public static boolean isIndicatorEnabled() {
        return KrylixConfig.get().healthIndicatorEnabled;
    }

    /** Forget the target (left the world). */
    public static void reset() {
        currentTarget = null;
        targetState = null;
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
        if (!(looked instanceof LivingEntity) && mc.crosshairPickEntity instanceof LivingEntity) {
            looked = mc.crosshairPickEntity;
        }
        currentTarget = looked instanceof LivingEntity living && living.isAlive() && canShowPlate(living, mc.player) ? living : null;
        if (currentTarget == null) {
            targetState = null;
        }
    }

    /**
     * Mirrors vanilla LivingEntityRenderer.shouldShowName: no plate for spectators, for entities invisible to us, or
     * when a scoreboard team hides its nametags from us — the plate must not reveal what the game hides in PvP.
     */
    private static boolean canShowPlate(LivingEntity entity, LocalPlayer viewer) {
        if (entity.isSpectator()) {
            return false;
        }
        boolean visible = !entity.isInvisibleTo(viewer);
        Team team = entity.getTeam();
        if (team == null) {
            return visible;
        }
        Team mine = viewer.getTeam();
        return switch (team.getNameTagVisibility()) {
            case ALWAYS -> visible;
            case NEVER -> false;
            case HIDE_FOR_OTHER_TEAMS -> mine == null ? visible : team.isAlliedTo(mine) && (team.canSeeFriendlyInvisibles() || visible);
            case HIDE_FOR_OWN_TEAM -> mine == null ? visible : !team.isAlliedTo(mine) && visible;
        };
    }

    private static Entity getEntityLookedAt(Entity e) {
        Entity foundEntity = null;
        final double finalDistance = MAX_DISTANCE;
        HitResult pos = raycast(e, finalDistance);
        Vec3 positionVector = e.getEyePosition();

        double distance = pos.getLocation().distanceTo(positionVector);

        Vec3 lookVector = e.getLookAngle();
        Vec3 reachVector = positionVector.add(lookVector.x * finalDistance, lookVector.y * finalDistance, lookVector.z * finalDistance);

        AABB searchBox = e.getBoundingBox().expandTowards(lookVector.scale(finalDistance)).inflate(1.0);
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

    /**
     * During render-state extraction: returns true for the crosshair target, after making sure its state has a name
     * and a nametag anchor, so the loader lets its nametag render (the plate replaces it in {@link #onSubmitNameDisplay}).
     */
    public static boolean applyNameplate(Entity entity, EntityRenderState state, float partialTick) {
        if (!isIndicatorEnabled() || !(entity instanceof LivingEntity living) || !living.isAlive()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || entity == mc.player) return false;

        // The target is picked once per client tick (KrylixClient.onTick), not for every entity every frame
        if (currentTarget != living) {
            if (targetState == state) {
                targetState = null;
            }
            return false;
        }
        targetState = state;
        if (state.nameTag == null) {
            state.nameTag = living.getDisplayName();
        }
        Vec3 attach = state.nameTagAttachment;
        if (attach == null) {
            attach = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getYRot(partialTick));
            if (attach == null) {
                attach = new Vec3(0, entity.getBbHeight() + 0.5, 0);
            }
        }
        state.nameTagAttachment = attach;
        return true;
    }

    public static boolean onSubmitNameDisplay(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        if (!isIndicatorEnabled()) return false;
        LivingEntity living = currentTarget;
        // Only the target's own nametag: every other name (players, named mobs) is left to vanilla
        if (living == null || !living.isAlive() || state != targetState || state.nameTagAttachment == null) return false;

        renderTwoLineHealthNameplate(state, poseStack, submitNodeCollector, cameraRenderState, living);
        return true;
    }

    public static void renderTwoLineHealthNameplate(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, LivingEntity living) {
        Vec3 attach = state.nameTagAttachment;
        if (attach == null) return;

        // Lift both lines by +0.35 blocks above the entity
        Vec3 liftedAttach = attach.add(0, 0.35, 0);

        // Line 1: Mob Name (at yOffset = 0, full bright, high above mob)
        Component name = state.nameTag != null ? state.nameTag : living.getDisplayName();
        submitNodeCollector.submitNameTag(poseStack, liftedAttach, 0, name, !state.isDiscrete, LightCoordsUtil.FULL_BRIGHT, cameraRenderState);

        // Line 2: Hearts + Text (at yOffset = 10, full bright, right below mob name)
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

        submitNodeCollector.submitNameTag(poseStack, liftedAttach, 10, healthLine, !state.isDiscrete, LightCoordsUtil.FULL_BRIGHT, cameraRenderState);
    }
}
