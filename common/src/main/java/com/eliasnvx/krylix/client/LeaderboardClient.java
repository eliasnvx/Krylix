package com.eliasnvx.krylix.client;

import net.minecraft.util.Util;
import com.eliasnvx.krylix.network.KrylixPayloads;
import com.eliasnvx.krylix.network.KrylixPayloads.LeaderboardPayload;
import com.eliasnvx.krylix.network.KrylixPayloads.LeaderboardRow;
import com.eliasnvx.krylix.platform.KrylixPlatform;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The leaderboard the server sent last, and whether we are waiting for one. Client thread only. */
public final class LeaderboardClient {
    public enum State { EMPTY, LOADING, READY, UNAVAILABLE }

    private static State state = State.EMPTY;
    private static List<LeaderboardRow> byKills = List.of();
    private static List<LeaderboardRow> byMobKills = List.of();
    private static int totalPlayers;
    /** Bumped whenever the data changes, so an open screen knows to reset its scroll. */
    private static int version;
    private static long requestedAt;
    /** No answer this long after a request (the server's cooldown dropped it): ask again. */
    private static final long RETRY_MS = 2_000;

    private LeaderboardClient() {
    }

    /** Ask the server for the table (the screen does this when it opens). */
    public static void request() {
        KrylixPlatform platform = KrylixPlatform.get();
        if (platform.canSendToServer(KrylixPayloads.LeaderboardRequestPayload.TYPE)) {
            platform.sendToServer(KrylixPayloads.LeaderboardRequestPayload.INSTANCE);
            requestedAt = Util.getMillis();
            if (state != State.READY) {
                state = State.LOADING;
            }
        } else if (state != State.READY) {
            state = State.UNAVAILABLE; // the server doesn't run Krylix
        }
    }

    /** Called every tick while the screen is open: a request that got no answer is sent again. */
    public static void retryIfStuck() {
        if (state == State.LOADING && Util.getMillis() - requestedAt > RETRY_MS) {
            request();
        }
    }

    public static void apply(LeaderboardPayload payload) {
        set(payload.rows(), payload.totalPlayers());
    }

    /** Also used by the test-fill command and the docs screenshots. */
    public static void set(List<LeaderboardRow> rows, int total) {
        List<LeaderboardRow> kills = new ArrayList<>(rows);
        kills.sort(Comparator.comparingInt(LeaderboardRow::kills).reversed()
            .thenComparingInt(LeaderboardRow::deaths)
            .thenComparing(LeaderboardRow::name, String.CASE_INSENSITIVE_ORDER));
        List<LeaderboardRow> mobs = new ArrayList<>(rows);
        mobs.sort(Comparator.comparingInt(LeaderboardRow::mobKills).reversed()
            .thenComparing(LeaderboardRow::name, String.CASE_INSENSITIVE_ORDER));
        byKills = List.copyOf(kills);
        byMobKills = List.copyOf(mobs);
        totalPlayers = total;
        state = State.READY;
        version++;
    }

    public static void clear() {
        byKills = List.of();
        byMobKills = List.of();
        totalPlayers = 0;
        state = State.EMPTY;
        version++;
    }

    public static State state() {
        return state;
    }

    public static List<LeaderboardRow> byKills() {
        return byKills;
    }

    public static List<LeaderboardRow> byMobKills() {
        return byMobKills;
    }

    public static int totalPlayers() {
        return totalPlayers;
    }

    public static int version() {
        return version;
    }
}
