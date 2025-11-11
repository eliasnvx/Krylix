package com.example.modid.forge.client

import com.example.modid.krylix.Krylix
import net.minecraftforge.client.event.RenderGuiEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.api.distmarker.Dist

/**
 * Client-side event handlers for Krylix mod
 */
@Mod.EventBusSubscriber(modid = Krylix.MOD_ID, value = [Dist.CLIENT], bus = Mod.EventBusSubscriber.Bus.FORGE)
object ClientEvents {
    
    init {
        println("ClientEvents loaded!")
        Krylix.LOGGER.info("ClientEvents class initialized successfully")
    }
    
    /**
     * Обработчик рендеринга GUI для отображения kill feed HUD
     */
    @SubscribeEvent
    @JvmStatic
    fun onRenderGui(event: RenderGuiEvent.Post) {
        KillFeedHud.render(event.guiGraphics, event.partialTick)
    }
}
