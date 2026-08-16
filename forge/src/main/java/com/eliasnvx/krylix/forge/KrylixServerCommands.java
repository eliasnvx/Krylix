package com.eliasnvx.krylix.forge;

import com.eliasnvx.krylix.Krylix;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class KrylixServerCommands {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("krylix")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("toggle")
                    .executes(context -> {
                        boolean current = KillFeedManager.isEnabled();
                        KillFeedManager.setEnabled(!current);
                        context.getSource().sendSuccess(
                            () -> Component.translatable("krylix.command.killfeed_state", !current ? Component.translatable("krylix.toggle.on") : Component.translatable("krylix.toggle.off")),
                            true
                        );
                        return 1;
                    })
                )
                .then(Commands.literal("status")
                    .executes(context -> {
                        context.getSource().sendSuccess(
                            () -> Component.translatable(
                                "krylix.command.status",
                                KillFeedManager.isEnabled() ? Component.translatable("krylix.toggle.on") : Component.translatable("krylix.toggle.off"),
                                0
                            ),
                            false
                        );
                        return 1;
                    })
                )
        );
    }
}
