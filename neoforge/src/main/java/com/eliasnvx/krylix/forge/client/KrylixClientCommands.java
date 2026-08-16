package com.eliasnvx.krylix.forge.client;

import com.eliasnvx.krylix.Krylix;
import com.eliasnvx.krylix.forge.network.NetworkPackets.PlayerStatEntry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@EventBusSubscriber(modid = Krylix.MOD_ID, value = Dist.CLIENT)
public class KrylixClientCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("krylix")
                .then(
                    Commands.literal("hud")
                        .then(
                            Commands.literal("toggle")
                                .executes(context -> {
                                    KillFeedHud.setEnabled(!KillFeedHud.isHudEnabled());
                                    Component state = Component.translatable(KillFeedHud.isHudEnabled() ? "krylix.toggle.on" : "krylix.toggle.off");
                                    context.getSource().sendSuccess(
                                        () -> Component.translatable("krylix.command.hud_state", state),
                                        true
                                    );
                                    return 1;
                                })
                        )
                        .then(
                            Commands.literal("clear")
                                .executes(context -> {
                                    KillFeedHud.clearNotifications();
                                    context.getSource().sendSuccess(
                                        () -> Component.translatable("krylix.command.hud_cleared"),
                                        true
                                    );
                                    return 1;
                                })
                        )
                        .then(
                            Commands.literal("count")
                                .executes(context -> {
                                    context.getSource().sendSuccess(
                                        () -> Component.translatable("krylix.command.hud_count", KillFeedHud.getNotificationCount()),
                                        true
                                    );
                                    return 1;
                                })
                        )
                )
                .then(
                    Commands.literal("leaderboard")
                        .then(
                            Commands.literal("testfill")
                                .executes(context -> {
                                    int count = fillTestLeaderboard(60);
                                    context.getSource().sendSuccess(
                                        () -> Component.translatable("krylix.command.leaderboard_filled", count),
                                        true
                                    );
                                    return 1;
                                })
                                .then(
                                    Commands.argument("count", IntegerArgumentType.integer(1, 500))
                                        .executes(context -> {
                                            int requested = IntegerArgumentType.getInteger(context, "count");
                                            int count = fillTestLeaderboard(requested);
                                            context.getSource().sendSuccess(
                                                () -> Component.translatable("krylix.command.leaderboard_filled", count),
                                                true
                                            );
                                            return 1;
                                        })
                                )
                        )
                        .then(
                            Commands.literal("clear")
                                .executes(context -> {
                                    PlayerStatsClient.updateStats(new ArrayList<>());
                                    context.getSource().sendSuccess(
                                        () -> Component.translatable("krylix.command.leaderboard_cleared"),
                                        true
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
