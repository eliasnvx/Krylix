package com.eliasnvx.krylix.fabric.mixin;

import com.eliasnvx.krylix.fabric.client.HealthIndicator;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
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
}
