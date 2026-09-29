package com.eliasnvx.krylix.neoforge.client;

import com.eliasnvx.krylix.Krylix;
import com.eliasnvx.krylix.client.HealthIndicator;
import com.eliasnvx.krylix.client.KrylixClient;
import com.eliasnvx.krylix.client.KrylixClientCommands;
import com.eliasnvx.krylix.client.KrylixKeyBindings;
import com.eliasnvx.krylix.config.ModConfig;
import me.shedaniel.autoconfig.AutoConfigClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;
import net.minecraft.util.TriState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Krylix.MOD_ID, dist = Dist.CLIENT)
public final class KrylixNeoForgeClient {
    public KrylixNeoForgeClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
            (mod, parent) -> AutoConfigClient.getConfigScreen(ModConfig.class, parent).get());

        modBus.addListener((RegisterKeyMappingsEvent event) -> {
            event.registerCategory(KrylixKeyBindings.CATEGORY);
            for (KeyMapping key : KrylixKeyBindings.ALL) {
                event.register(key);
            }
        });
        // Under the chat, so chat lines stay readable over the feed
        modBus.addListener((RegisterGuiLayersEvent event) -> event.registerBelow(VanillaGuiLayers.CHAT,
            Identifier.fromNamespaceAndPath(Krylix.MOD_ID, "hud"), (graphics, deltaTracker) -> KrylixClient.renderHud(graphics)));

        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> KrylixClient.onTick(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> KrylixClient.onDisconnect());
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Render.Post event) -> {
            if (event.getScreen() instanceof DeathScreen screen) {
                KrylixClient.renderDeathScreen(event.getGuiGraphics(), screen.width, screen.height);
            }
        });
        NeoForge.EVENT_BUS.addListener((RenderNameTagEvent.CanRender event) -> {
            if (HealthIndicator.applyNameplate(event.getEntity(), event.getEntityRenderState(), event.getPartialTick())) {
                if (event.getContent() == null) {
                    event.setContent(event.getEntity().getDisplayName());
                }
                event.setCanRender(TriState.TRUE);
            }
        });
        NeoForge.EVENT_BUS.addListener((RenderNameTagEvent.DoRender event) -> {
            if (HealthIndicator.onSubmitNameDisplay(event.getEntityRenderState(), event.getPoseStack(), event.getSubmitNodeCollector(), event.getCameraRenderState())) {
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) -> event.getDispatcher().register(
            KrylixClientCommands.<CommandSourceStack>build((source, message) -> source.sendSuccess(() -> message, false))));
    }
}
