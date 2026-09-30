package com.eliasnvx.krylix.fabric.mixin;

import com.eliasnvx.krylix.client.HealthIndicator;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void krylix$onExtractRenderState(T entity, S state, float partialTick, CallbackInfo ci) {
        HealthIndicator.applyNameplate(entity, state, partialTick);
    }

    /**
     * The final, 5-argument name display every renderer ends in: players' AvatarRenderer overrides the 4-argument one
     * without calling super, so hooking that one would skip every player.
     */
    @Inject(method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;I)V", at = @At("HEAD"), cancellable = true)
    private void krylix$onSubmitNameDisplay(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                                            CameraRenderState cameraRenderState, int offset, CallbackInfo ci) {
        if (HealthIndicator.onSubmitNameDisplay(state, poseStack, submitNodeCollector, cameraRenderState, offset, true)) {
            ci.cancel();
        }
    }
}
