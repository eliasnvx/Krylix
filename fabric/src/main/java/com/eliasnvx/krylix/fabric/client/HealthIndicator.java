package com.eliasnvx.krylix.fabric.client;

import com.eliasnvx.krylix.fabric.config.KrylixConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

public class HealthIndicator {
    private static final double MAX_DISTANCE = 48.0;
    private static final Identifier HEART_CONTAINER = Identifier.fromNamespaceAndPath("minecraft", "textures/gui/sprites/hud/heart/container.png");
    private static final Identifier HEART_FULL = Identifier.fromNamespaceAndPath("minecraft", "textures/gui/sprites/hud/heart/full.png");
    private static final Identifier HEART_HALF = Identifier.fromNamespaceAndPath("minecraft", "textures/gui/sprites/hud/heart/half.png");

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

    public static void applyNameplate(Entity entity, EntityRenderState state, float partialTick) {
        if (!isIndicatorEnabled() || !(entity instanceof LivingEntity living) || !living.isAlive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || entity == mc.player) return;

        updateTarget();
        if (currentTarget == living) {
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
            state.nameTagAttachment = attach.add(0, 0.35, 0);
        }
    }

    public static void onSubmitNameDisplay(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        if (!isIndicatorEnabled()) return;
        LivingEntity living = currentTarget;
        if (living == null || !living.isAlive() || state.nameTagAttachment == null) return;

        renderTexturedHealth(state, poseStack, submitNodeCollector, cameraRenderState, living);
    }

    public static void renderTexturedHealth(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, LivingEntity living) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        float maxHp = Math.max(1f, living.getMaxHealth());
        float hp = Math.max(0f, Math.min(maxHp, living.getHealth()));
        int hpRounded = Math.round(hp);
        int maxHpRounded = Math.round(maxHp);

        int totalHearts = Math.min(10, Math.max(1, (int) Math.ceil(maxHp / 2.0)));
        float hpPerHeart = maxHp / (float) totalHearts;
        int fullHearts = (int) (hp / hpPerHeart);
        boolean hasHalf = (hp - (fullHearts * hpPerHeart)) >= (hpPerHeart * 0.25f);

        int heartsWidth = (totalHearts - 1) * 8 + 9;
        String text = hpRounded + "/" + maxHpRounded;
        int textWidth = font.width(text);
        int spacing = 4;
        int totalWidth = heartsWidth + spacing + textWidth;
        float startX = -totalWidth / 2.0f;

        poseStack.pushPose();
        Vec3 attachment = state.nameTagAttachment;
        poseStack.translate(attachment.x, attachment.y + 0.5, attachment.z);
        poseStack.mulPose(cameraRenderState.orientation);
        poseStack.scale(0.025f, -0.025f, 0.025f);

        final float fStartX = startX;
        final int fTotalHearts = totalHearts;
        final int fFullHearts = fullHearts;
        final boolean fHasHalf = hasHalf;

        // 1. Containers (empty heart frames) using unshaded text render type
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.text(HEART_CONTAINER), (pose, buffer) -> {
            for (int i = 0; i < fTotalHearts; i++) {
                float hx = fStartX + i * 8;
                drawQuad(pose, buffer, hx, 10.0f, hx + 9, 19.0f);
            }
        });

        // 2. Full bright red hearts
        if (fullHearts > 0) {
            submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.text(HEART_FULL), (pose, buffer) -> {
                for (int i = 0; i < fFullHearts; i++) {
                    float hx = fStartX + i * 8;
                    drawQuad(pose, buffer, hx, 10.0f, hx + 9, 19.0f);
                }
            });
        }

        // 3. Half heart
        if (hasHalf && fullHearts < totalHearts) {
            final int halfIndex = fullHearts;
            submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.text(HEART_HALF), (pose, buffer) -> {
                float hx = fStartX + halfIndex * 8;
                drawQuad(pose, buffer, hx, 10.0f, hx + 9, 19.0f);
            });
        }

        // 4. Numbers (8/8)
        float textX = startX + heartsWidth + spacing;
        float textY = 10.5f;
        FormattedCharSequence seq = Component.literal(text).getVisualOrderText();
        submitNodeCollector.submitText(poseStack, textX, textY, seq, true, Font.DisplayMode.SEE_THROUGH, 0xFFFFFFFF, 0x40000000, LightCoordsUtil.FULL_BRIGHT, 0);

        poseStack.popPose();
    }

    private static void drawQuad(PoseStack.Pose pose, VertexConsumer buffer, float x1, float y1, float x2, float y2) {
        Matrix4f mat = pose.pose();
        int fullLight = LightCoordsUtil.FULL_BRIGHT;
        buffer.addVertex(mat, x1, y2, 0.0F).setColor(255, 255, 255, 255).setUv(0.0F, 1.0F).setLight(fullLight);
        buffer.addVertex(mat, x2, y2, 0.0F).setColor(255, 255, 255, 255).setUv(1.0F, 1.0F).setLight(fullLight);
        buffer.addVertex(mat, x2, y1, 0.0F).setColor(255, 255, 255, 255).setUv(1.0F, 0.0F).setLight(fullLight);
        buffer.addVertex(mat, x1, y1, 0.0F).setColor(255, 255, 255, 255).setUv(0.0F, 0.0F).setLight(fullLight);
    }
}
