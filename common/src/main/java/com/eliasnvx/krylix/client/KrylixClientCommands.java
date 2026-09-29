package com.eliasnvx.krylix.client;

import com.eliasnvx.krylix.network.KrylixPayloads.LeaderboardRow;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * /krylixclient — client-side commands. Built for any command source type, so each loader can register it with
 * its own client command API and give us a way to send feedback.
 */
public final class KrylixClientCommands {
    public static final String ROOT = "krylixclient";

    private KrylixClientCommands() {
    }

    public static <S> LiteralArgumentBuilder<S> build(BiConsumer<S, Component> feedback) {
        return LiteralArgumentBuilder.<S>literal(ROOT)
            .then(LiteralArgumentBuilder.<S>literal("hud")
                .then(LiteralArgumentBuilder.<S>literal("toggle").executes(ctx -> {
                    boolean enabled = !KillFeedHud.isHudEnabled();
                    KillFeedHud.setEnabled(enabled);
                    feedback.accept(ctx.getSource(), Component.translatable("krylix.toggle.killfeed",
                        Component.translatable(enabled ? "krylix.toggle.on" : "krylix.toggle.off")));
                    return 1;
                }))
                .then(LiteralArgumentBuilder.<S>literal("clear").executes(ctx -> {
                    KillFeedHud.clear();
                    feedback.accept(ctx.getSource(), Component.translatable("krylix.command.hud_cleared"));
                    return 1;
                }))
                .then(LiteralArgumentBuilder.<S>literal("count").executes(ctx -> {
                    feedback.accept(ctx.getSource(), Component.translatable("krylix.command.hud_count", KillFeedHud.count()));
                    return 1;
                })))
            .then(LiteralArgumentBuilder.<S>literal("leaderboard")
                .then(LiteralArgumentBuilder.<S>literal("testfill")
                    .executes(ctx -> {
                        feedback.accept(ctx.getSource(), Component.translatable("krylix.command.leaderboard_filled", fillTestLeaderboard(60)));
                        return 1;
                    })
                    .then(RequiredArgumentBuilder.<S, Integer>argument("count", IntegerArgumentType.integer(1, 500)).executes(ctx -> {
                        int count = fillTestLeaderboard(IntegerArgumentType.getInteger(ctx, "count"));
                        feedback.accept(ctx.getSource(), Component.translatable("krylix.command.leaderboard_filled", count));
                        return 1;
                    })))
                .then(LiteralArgumentBuilder.<S>literal("clear").executes(ctx -> {
                    LeaderboardClient.clear();
                    feedback.accept(ctx.getSource(), Component.translatable("krylix.command.leaderboard_cleared"));
                    return 1;
                })));
    }

    /** Random rows to look at scrolling and paging without a busy server. Client only, not saved. */
    private static int fillTestLeaderboard(int count) {
        String[] names = {
            "Notch", "Jeb_", "Dinnerbone", "Grumm", "C418", "Herobrine", "Dream", "Technoblade",
            "Ph1LzA", "Tommyinnit", "Wilbur", "Tubbo", "Ranboo", "Sapnap", "GeorgeNotFound",
            "BadBoyHalo", "Skeppy", "Quackity", "Karl_Jacobs", "Fundy", "Purpled", "Antfrost",
            "Awesamdude", "HBomb94", "Ponk", "Vikkstar", "Slimecicle", "Callahan", "ZombieCleo", "Punz"
        };
        Random random = new Random();
        List<LeaderboardRow> rows = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String name = i < names.length ? names[i] : "TestPlayer" + (i + 1);
            rows.add(new LeaderboardRow(UUID.randomUUID(), name, random.nextInt(61), random.nextInt(41), random.nextInt(121)));
        }
        LeaderboardClient.set(rows, rows.size());
        return rows.size();
    }
}
