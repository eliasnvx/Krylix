package com.eliasnvx.krylix.forge.client

import com.eliasnvx.krylix.Krylix
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraftforge.client.event.RenderGuiEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent

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
        MinecraftForge.EVENT_BUS.register(HealthIndicator)

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

        // Рендерим kill feed и статистику убийств мобов
        KillFeedHud.render(event.guiGraphics, event.partialTick)
        MobStatsHud.render(event.guiGraphics, event.partialTick)
    }

    /**
     * Обрабатывает нажатия клавиш быстрого вкл/выкл HUD-элементов
     */
    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) return

        while (KrylixKeyBindings.toggleKillFeed.consumeClick()) {
            val enabled = !KillFeedHud.isHudEnabled()
            KillFeedHud.setEnabled(enabled)
            notifyToggle("krylix.toggle.killfeed", enabled)
        }
        while (KrylixKeyBindings.toggleMobStats.consumeClick()) {
            val enabled = !MobStatsHud.isStatsEnabled()
            MobStatsHud.setEnabled(enabled)
            notifyToggle("krylix.toggle.mobstats", enabled)
        }
        while (KrylixKeyBindings.openLeaderboard.consumeClick()) {
            val minecraft = Minecraft.getInstance()
            if (minecraft.screen == null) {
                minecraft.setScreen(LeaderboardScreen())
            }
        }
        while (KrylixKeyBindings.toggleHealthIndicator.consumeClick()) {
            val enabled = !HealthIndicator.isIndicatorEnabled()
            HealthIndicator.setEnabled(enabled)
            notifyToggle("krylix.toggle.health_indicator", enabled)
        }

        HealthIndicator.updateTarget()
    }

    /**
     * Звук + action bar сообщение (над баром еды/здоровья) при переключении HUD-элемента клавишей —
     * иначе непонятно, сработало ли нажатие, когда элемент и так был не виден на экране.
     */
    private fun notifyToggle(translationKey: String, enabled: Boolean) {
        val minecraft = Minecraft.getInstance()
        val stateText = Component.translatable(if (enabled) "krylix.toggle.on" else "krylix.toggle.off")
        minecraft.player?.displayClientMessage(Component.translatable(translationKey, stateText), true)
        minecraft.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
    }
}
