package com.eliasnvx.krylix.server;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** /krylix toggle | status — operators only. */
public final class KrylixServerCommands {
    private KrylixServerCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("krylix")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("toggle").executes(context -> {
                    KrylixServer.setFeedEnabled(!KrylixServer.isFeedEnabled());
                    context.getSource().sendSuccess(
                        () -> Component.translatable("krylix.command.killfeed_state", onOff(KrylixServer.isFeedEnabled())), true);
                    return 1;
                }))
                .then(Commands.literal("status").executes(context -> {
                    int players = PlayerKillStatsData.get(context.getSource().getServer()).stats.size();
                    context.getSource().sendSuccess(
                        () -> Component.translatable("krylix.command.status", onOff(KrylixServer.isFeedEnabled()), players), false);
                    return 1;
                }))
        );
    }

    private static Component onOff(boolean on) {
        return Component.translatable(on ? "krylix.toggle.on" : "krylix.toggle.off");
    }
}
