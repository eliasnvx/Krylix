package com.eliasnvx.krylix.forge.mixin;

import com.eliasnvx.krylix.forge.client.KillFeedHud;
import com.eliasnvx.krylix.forge.client.MobStatsHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void krylix$onRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        KillFeedHud.render(guiGraphics, deltaTracker.getGameTimeDeltaTicks());
        MobStatsHud.render(guiGraphics, deltaTracker.getGameTimeDeltaTicks());
    }
}
