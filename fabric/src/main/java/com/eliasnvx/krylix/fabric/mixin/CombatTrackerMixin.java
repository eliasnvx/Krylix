package com.eliasnvx.krylix.fabric.mixin;

import com.eliasnvx.krylix.server.KrylixServer;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Damage for the death recap. {@code LivingEntity#actuallyHurt} records here the health really lost: after armor,
 * enchantments, invulnerability frames and absorption. Fabric's AFTER_DAMAGE reports the damage before armor, which
 * would make the recap's number differ from NeoForge's.
 */
@Mixin(CombatTracker.class)
abstract class CombatTrackerMixin {
    @Shadow
    @Final
    private LivingEntity mob;

    @Inject(method = "recordDamage", at = @At("HEAD"))
    private void krylix$recordDamage(DamageSource source, float damage, CallbackInfo ci) {
        KrylixServer.onDamage(mob, source, damage);
    }
}
