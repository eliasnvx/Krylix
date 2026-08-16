package com.eliasnvx.krylix.fabric.client;

import com.eliasnvx.krylix.Krylix;
import com.eliasnvx.krylix.fabric.network.FabricNetworkPackets.PlayerStatEntry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class KrylixClientCommands {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register(KrylixClientCommands::onRegisterCommands);
    }

    private static void onRegisterCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(
            ClientCommandManager.literal("krylixclient")
                .then(
                    ClientCommandManager.literal("hud")
                        .then(
                            ClientCommandManager.literal("toggle")
                                .executes(context -> {
                                    KillFeedHud.setEnabled(!KillFeedHud.isHudEnabled());
                                    Component state = Component.translatable(KillFeedHud.isHudEnabled() ? "krylix.toggle.on" : "krylix.toggle.off");
                                    context.getSource().sendFeedback(
                                        Component.translatable("krylix.command.hud_state", state)
                                    );
                                    return 1;
                                })
                        )
                        .then(
                            ClientCommandManager.literal("clear")
                                .executes(context -> {
                                    KillFeedHud.clearNotifications();
                                    context.getSource().sendFeedback(
                                        Component.translatable("krylix.command.hud_cleared")
                                    );
                                    return 1;
                                })
                        )
                        .then(
                            ClientCommandManager.literal("count")
                                .executes(context -> {
                                    context.getSource().sendFeedback(
                                        Component.translatable("krylix.command.hud_count", KillFeedHud.getNotificationCount())
                                    );
                                    return 1;
                                })
                        )
                )
                .then(
                    ClientCommandManager.literal("leaderboard")
                        .then(
                            ClientCommandManager.literal("testfill")
                                .executes(context -> {
                                    int count = fillTestLeaderboard(60);
                                    context.getSource().sendFeedback(
                                        Component.translatable("krylix.command.leaderboard_filled", count)
                                    );
                                    return 1;
                                })
                                .then(
                                    ClientCommandManager.argument("count", IntegerArgumentType.integer(1, 500))
                                        .executes(context -> {
                                            int requested = IntegerArgumentType.getInteger(context, "count");
                                            int count = fillTestLeaderboard(requested);
                                            context.getSource().sendFeedback(
                                                Component.translatable("krylix.command.leaderboard_filled", count)
                                            );
                                            return 1;
                                        })
                                )
                        )
                        .then(
                            ClientCommandManager.literal("clear")
                                .executes(context -> {
                                    PlayerStatsClient.updateStats(new ArrayList<>());
                                    context.getSource().sendFeedback(
                                        Component.translatable("krylix.command.leaderboard_cleared")
                                    );
                                    return 1;
                                })
                        )
                )
        );

        Krylix.LOGGER.debug("Krylix client commands registered");
    }

    private static int fillTestLeaderboard(int requestedCount) {
        String[] baseNames = {
            "Notch", "Jeb_", "Dinnerbone", "Grumm", "C418", "Herobrine", "Dream", "Technoblade",
            "Ph1LzA", "Tommyinnit", "Wilbur", "Tubbo", "Ranboo", "Sapnap", "GeorgeNotFound",
            "BadBoyHalo", "Skeppy", "Quackity", "Karl_Jacobs", "Fundy", "Purpled", "Antfrost",
            "Awesamdude", "HBomb94", "Ponk", "Vikkstar", "Slimecicle", "Callahan", "ZombieCleo", "Punz"
        };
        int count = Math.max(1, Math.min(requestedCount, 500));
        List<PlayerStatEntry> entries = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < count; i++) {
            String name = i < baseNames.length ? baseNames[i] : "TestPlayer" + (i + 1);
            entries.add(new PlayerStatEntry(
                UUID.randomUUID().toString(),
                name,
                random.nextInt(61),
                random.nextInt(41),
                random.nextInt(121)
            ));
        }

        PlayerStatsClient.updateStats(entries);
        return entries.size();
    }
}
