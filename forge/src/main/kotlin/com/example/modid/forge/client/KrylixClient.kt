package com.example.modid.forge.client

import com.example.modid.krylix.Krylix
import net.minecraftforge.client.event.RenderGuiEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

/**
 * Client-side event handlers только для клиента
 * Предотвращает крэш на dedicated server
 */
object KrylixClient {
    
    init {
        println("KrylixClient loaded!")
        Krylix.LOGGER.info("KrylixClient class initialized successfully")
    }
    
    /**
     * Ручная регистрация client-side событий
     * Вызывается из главного класса мода только на клиенте
     */
    fun registerClientEvents() {
        println("KrylixClient: Registering client events manually!")
        Krylix.LOGGER.info("Registering Krylix client events manually")
        
        MinecraftForge.EVENT_BUS.register(this)
        
        Krylix.LOGGER.info("Krylix client events registered successfully")
    }
    
    /**
     * Рендерит kill feed HUD на экране
     */
    @SubscribeEvent
    fun onRenderGui(event: RenderGuiEvent.Post) {
        // Проверяем, что мы в игре и не в меню
        val minecraft = net.minecraft.client.Minecraft.getInstance()
        if (minecraft.screen != null) return
        
        // Рендерим kill feed
        KillFeedHud.render(event.guiGraphics, event.partialTick)
    }
}
