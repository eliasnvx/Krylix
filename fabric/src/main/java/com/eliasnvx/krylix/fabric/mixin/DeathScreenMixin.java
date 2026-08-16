package com.eliasnvx.krylix.fabric.mixin;

import com.eliasnvx.krylix.fabric.client.DeathRecapClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin extends Screen {
    protected DeathScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void krylix$onRenderDeathScreen(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (DeathRecapClient.hasActiveRecap()) {
            DeathRecapClient.render(guiGraphics, this.width, this.height);
        }
    }
}
