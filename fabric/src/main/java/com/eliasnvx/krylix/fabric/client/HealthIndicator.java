package com.eliasnvx.krylix.fabric.client;

import com.eliasnvx.krylix.core.HealthBarStyle;
import com.eliasnvx.krylix.fabric.config.KrylixConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
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
    private static final int BAR_SEGMENTS = 10;

    private static LivingEntity currentTarget = null;

    public static void setEnabled(boolean enabled) {
        KrylixConfig.get().healthIndicatorEnabled = enabled;
        KrylixConfig.save();
    }

    public static boolean isIndicatorEnabled() {
        return KrylixConfig.get().healthIndicatorEnabled;
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
                AABB collisionBox = entity.getBoundingBoxForCulling().inflate(0.5);
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

    public static void onRenderWorld(WorldRenderContext context) {
        if (!KrylixConfig.get().healthIndicatorEnabled) return;

        updateTarget();

        LivingEntity target = currentTarget;
        if (target == null || !target.isAlive()) return;

        render(context, target);
    }

    private static void render(WorldRenderContext context, LivingEntity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = context.camera();
        Vec3 camPos = camera.getPosition();

        float partialTick = context.tickCounter().getGameTimeDeltaPartialTick(false);
        double entityX = Mth.lerp(partialTick, entity.xOld, entity.getX());
        double entityY = Mth.lerp(partialTick, entity.yOld, entity.getY());
        double entityZ = Mth.lerp(partialTick, entity.zOld, entity.getZ());

        Vec3 attachment = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getViewYRot(partialTick));
        double attachX = attachment != null ? attachment.x : 0.0;
        double attachY = attachment != null ? attachment.y + 0.5 : entity.getBbHeight() + 0.5;
        double attachZ = attachment != null ? attachment.z : 0.0;

        PoseStack poseStack = context.matrixStack();
        poseStack.pushPose();
        poseStack.translate(entityX - camPos.x + attachX, entityY - camPos.y + attachY, entityZ - camPos.z + attachZ);
        poseStack.mulPose(camera.rotation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180));
        poseStack.scale(-0.0267f, -0.0267f, 0.0267f);

        Font font = minecraft.font;
        float maxHealth = Math.max(1f, entity.getMaxHealth());
        float pct = Math.max(0f, Math.min(1f, entity.getHealth() / maxHealth));

        ChatFormatting color;
        if (pct > 0.6f) color = ChatFormatting.GREEN;
        else if (pct > 0.3f) color = ChatFormatting.YELLOW;
        else color = ChatFormatting.RED;

        int hp = Math.round(entity.getHealth());
        int maxHp = Math.round(maxHealth);
        Component hpText = Component.literal(barText(pct, hp, maxHp)).withStyle(color);
        Component nameText = entity.getDisplayName();

        boolean showOwnName = !entity.hasCustomName();

        Matrix4f matrix = poseStack.last().pose();
        int bgAlpha = 0x60000000;
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        int packedLight = 0xF000F0;

        if (showOwnName) {
            float nameWidth = font.width(nameText);
            font.drawInBatch(
                    nameText,
                    -nameWidth / 2f,
                    -10f,
                    -1,
                    false,
                    matrix,
                    bufferSource,
                    Font.DisplayMode.NORMAL,
                    bgAlpha,
                    packedLight
            );
        }

        float hpWidth = font.width(hpText);
        font.drawInBatch(
                hpText,
                -hpWidth / 2f,
                0f,
                -1,
                false,
                matrix,
                bufferSource,
                Font.DisplayMode.NORMAL,
                bgAlpha,
                packedLight
        );

        bufferSource.endBatch();
        poseStack.popPose();
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
