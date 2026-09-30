package com.eliasnvx.krylix.fabric.client;

import com.eliasnvx.krylix.Krylix;
import com.eliasnvx.krylix.client.KrylixClient;
import com.eliasnvx.krylix.client.KrylixClientApiImpl;
import com.eliasnvx.krylix.client.KrylixClientCommands;
import com.eliasnvx.krylix.client.KrylixKeyBindings;
import com.eliasnvx.krylix.client.MobHeads;
import com.eliasnvx.krylix.network.KrylixPayloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

public final class KrylixFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        for (KrylixPayloads.Entry<?> entry : KrylixPayloads.CLIENTBOUND) {
            registerReceiver(entry.type());
        }
        // Fabric: vanilla's Category.register, which Fabric API hooks to sort modded categories
        for (KeyMapping key : KrylixKeyBindings.create(KeyMapping.Category.register(KrylixKeyBindings.CATEGORY_ID))) {
            KeyMappingHelper.registerKeyMapping(key);
        }
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registries) ->
            dispatcher.register(KrylixClientCommands.<FabricClientCommandSource>build(FabricClientCommandSource::sendFeedback)));

        ClientTickEvents.END_CLIENT_TICK.register(KrylixClient::onTick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> KrylixClient.onDisconnect());
        // Under the chat, so chat lines stay readable over the feed
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.fromNamespaceAndPath(Krylix.MOD_ID, "hud"),
            (graphics, deltaTracker) -> KrylixClient.renderHud(graphics));
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(MobHeads.RELOAD_LISTENER_ID, new MobHeads.Loader());
        KrylixClientApiImpl.init();
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof DeathScreen) {
                ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, partialTick) ->
                    KrylixClient.renderDeathScreen(graphics, s.width, s.height));
            }
        });
    }

    /** Fabric runs play payload handlers on the client thread. */
    private static <T extends CustomPacketPayload> void registerReceiver(CustomPacketPayload.Type<T> type) {
        ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) -> KrylixClient.handle(payload));
    }
}
