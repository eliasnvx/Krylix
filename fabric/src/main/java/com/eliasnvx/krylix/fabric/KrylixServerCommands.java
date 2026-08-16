package com.eliasnvx.krylix.fabric;

import com.eliasnvx.krylix.Krylix;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class KrylixServerCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(
            Commands.literal("krylix")
                .then(
                    Commands.literal("toggle")
                        .executes(context -> {
                            KillFeedManager.setEnabled(!KillFeedManager.isEnabled());
                            Component state = Component.translatable(KillFeedManager.isEnabled() ? "krylix.toggle.on" : "krylix.toggle.off");
                            context.getSource().sendSuccess(
                                () -> Component.translatable("krylix.command.killfeed_state", state),
                                true
                            );
                            return 1;
                        })
                )
                .then(
                    Commands.literal("status")
                        .executes(context -> {
                            Component serverStatus = Component.translatable(KillFeedManager.isEnabled() ? "krylix.toggle.on" : "krylix.toggle.off");
                            int count = KillFeedManager.getActiveNotifications().size();
                            context.getSource().sendSuccess(
                                () -> Component.translatable("krylix.command.status", serverStatus, count),
                                true
                            );
                            return 1;
                        })
                )
        );

        Krylix.LOGGER.debug("Krylix server commands registered");
    }
}
