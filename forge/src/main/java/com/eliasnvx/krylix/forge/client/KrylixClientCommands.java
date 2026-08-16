package com.eliasnvx.krylix.forge.client;

import com.eliasnvx.krylix.Krylix;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Krylix.MOD_ID, value = Dist.CLIENT)
public class KrylixClientCommands {
    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("krylixclient")
                .then(Commands.literal("hud")
                    .then(Commands.literal("toggle")
                        .executes(context -> {
                            boolean current = KillFeedHud.isHudEnabled();
                            KillFeedHud.setEnabled(!current);
                            context.getSource().sendSuccess(
                                () -> Component.translatable("krylix.command.hud_state", !current ? Component.translatable("krylix.toggle.on") : Component.translatable("krylix.toggle.off")),
                                false
                            );
                            return 1;
                        })
                    )
                    .then(Commands.literal("clear")
                        .executes(context -> {
                            KillFeedHud.clearNotifications();
                            context.getSource().sendSuccess(
                                () -> Component.translatable("krylix.command.hud_cleared"),
                                false
                            );
                            return 1;
                        })
                    )
                )
        );
    }
}
