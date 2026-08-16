package com.eliasnvx.krylix.forge.client;

import com.eliasnvx.krylix.forge.config.KrylixConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class HealthIndicator {
    private static final double MAX_RAYCAST_DISTANCE = 16.0;

    public static boolean isEnabled() {
        return KrylixConfig.get().healthIndicatorEnabled;
    }

    public static void setEnabled(boolean enabled) {
        KrylixConfig.get().healthIndicatorEnabled = enabled;
        KrylixConfig.save();
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (!isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.options.hideGui) return;

        LivingEntity target = getTargetedEntity(mc);
        if (target == null || target == mc.player || !target.isAlive()) return;

        Camera camera = event.getCamera();
        PoseStack poseStack = new PoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        renderHealthBar(target, camera, poseStack, bufferSource, mc.font);
    }

    private static LivingEntity getTargetedEntity(Minecraft mc) {
        Entity cameraEntity = mc.getCameraEntity();
        if (cameraEntity == null) return null;

        Vec3 eyePos = cameraEntity.getEyePosition(1.0f);
        Vec3 viewVec = cameraEntity.getViewVector(1.0f);
        Vec3 reachVec = eyePos.add(viewVec.scale(MAX_RAYCAST_DISTANCE));
        AABB searchBox = cameraEntity.getBoundingBox().expandTowards(viewVec.scale(MAX_RAYCAST_DISTANCE)).inflate(1.0);

        EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(
            cameraEntity,
            eyePos,
            reachVec,
            searchBox,
            entity -> !entity.isSpectator() && entity.isPickable(),
            MAX_RAYCAST_DISTANCE * MAX_RAYCAST_DISTANCE
        );

        if (hitResult != null && hitResult.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    private static void renderHealthBar(LivingEntity entity, Camera camera, PoseStack poseStack, MultiBufferSource bufferSource, Font font) {
        Vec3 camPos = camera.getPosition();
        double entityX = entity.getX();
        double entityY = entity.getY() + entity.getBbHeight() + 0.5;
        double entityZ = entity.getZ();

        double dx = entityX - camPos.x;
        double dy = entityY - camPos.y;
        double dz = entityZ - camPos.z;

        float hp = Math.max(0f, entity.getHealth());
        float maxHp = Math.max(1f, entity.getMaxHealth());
        float ratio = Math.min(1.0f, Math.max(0.0f, hp / maxHp));

        String hpText = String.format("%.1f/%.1f", hp, maxHp);
        String barStr = buildBar(ratio);
        Component text = Component.literal(barStr + " §c❤ §f" + hpText);

        poseStack.pushPose();
        poseStack.translate(dx, dy, dz);

        Quaternionf camRot = new Quaternionf(camera.rotation());
        camRot.rotateY((float) Math.PI);
        poseStack.mulPose(camRot);

        float scale = 0.025f;
        poseStack.scale(scale, -scale, scale);

        Matrix4f matrix = poseStack.last().pose();
        float textWidth = font.width(text);
        float xOffset = -textWidth / 2.0f;

        font.drawInBatch(
            text,
            xOffset,
            0f,
            0xFFFFFFFF,
            false,
            matrix,
            bufferSource,
            Font.DisplayMode.SEE_THROUGH,
            0x40000000,
            0xF000F0
        );

        poseStack.popPose();
    }

    private static String buildBar(float ratio) {
        int totalSegments = 10;
        int filled = Math.round(ratio * totalSegments);
        StringBuilder sb = new StringBuilder();

        String color = "§a";
        if (ratio < 0.25f) {
            color = "§c";
        } else if (ratio < 0.5f) {
            color = "§e";
        }

        sb.append(color);
        for (int i = 0; i < filled; i++) {
            sb.append("▮");
        }
        sb.append("§7");
        for (int i = filled; i < totalSegments; i++) {
            sb.append("▯");
        }
        return sb.toString();
    }
}
