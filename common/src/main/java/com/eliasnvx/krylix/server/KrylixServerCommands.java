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
                        () -> Component.translatableWithFallback("krylix.command.killfeed_state", "Kill feed: %s", onOff(KrylixServer.isFeedEnabled())), true);
                    return 1;
                }))
                .then(Commands.literal("status").executes(context -> {
                    int players = PlayerKillStatsData.get(context.getSource().getServer()).stats.size();
                    context.getSource().sendSuccess(
                        () -> Component.translatableWithFallback("krylix.command.status", "Kill feed: %s, players tracked: %s", onOff(KrylixServer.isFeedEnabled()), players), false);
                    return 1;
                }))
        );
    }

    private static Component onOff(boolean on) {
        // With fallbacks: operators on clients without Krylix get English instead of raw keys
        return on ? Component.translatableWithFallback("krylix.toggle.on", "§aON") : Component.translatableWithFallback("krylix.toggle.off", "§cOFF");
    }
}
