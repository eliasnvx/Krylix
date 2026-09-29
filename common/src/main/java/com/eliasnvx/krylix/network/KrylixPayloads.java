package com.eliasnvx.krylix.network;

import com.eliasnvx.krylix.Krylix;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Every Krylix payload. Decoders never trust sizes from the wire: strings and collections are bounded, so a
 * malformed or hostile packet fails fast instead of allocating a huge list.
 */
public final class KrylixPayloads {
    /** Longest player / entity name we send (custom names can be long; vanilla caps them far below this). */
    private static final int MAX_NAME = 256;
    private static final int MAX_ID = 256;
    /** Leaderboard rows per reply; the screen pages through 60 at a time. */
    public static final int MAX_LEADERBOARD_ROWS = 2000;
    /** Distinct mob types in one player's panel (vanilla has ~80 living types; modpacks have more). */
    public static final int MAX_MOB_TYPES = 1024;

    public static final int FLAG_CRITICAL = 1;
    public static final int FLAG_SMASH = 1 << 1;
    public static final int FLAG_LONGSHOT = 1 << 2;

    private KrylixPayloads() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String path) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Krylix.MOD_ID, path));
    }

    private static void writeNullableUtf(FriendlyByteBuf buf, @Nullable String value) {
        buf.writeUtf(value != null ? value : "", MAX_ID);
    }

    private static @Nullable String readNullableUtf(FriendlyByteBuf buf) {
        String value = buf.readUtf(MAX_ID);
        return value.isEmpty() ? null : value;
    }

    private static void writeNullableUuid(FriendlyByteBuf buf, @Nullable UUID uuid) {
        buf.writeBoolean(uuid != null);
        if (uuid != null) {
            buf.writeUUID(uuid);
        }
    }

    private static @Nullable UUID readNullableUuid(FriendlyByteBuf buf) {
        return buf.readBoolean() ? buf.readUUID() : null;
    }

    private static int readBoundedSize(FriendlyByteBuf buf, int max) {
        int size = buf.readVarInt();
        if (size < 0 || size > max) {
            throw new IllegalArgumentException("Krylix payload collection too large: " + size + " > " + max);
        }
        return size;
    }

    /**
     * One side of a kill: a player (uuid set) or any other entity (entity type id set). The name is what the
     * server shows — a custom name, or the entity type's name in the server's language.
     */
    public record Combatant(String name, @Nullable UUID uuid, @Nullable String entityType) {
        static final StreamCodec<FriendlyByteBuf, Combatant> CODEC = StreamCodec.of(
            (buf, c) -> {
                buf.writeUtf(c.name(), MAX_NAME);
                writeNullableUuid(buf, c.uuid());
                writeNullableUtf(buf, c.entityType());
            },
            buf -> new Combatant(buf.readUtf(MAX_NAME), readNullableUuid(buf), readNullableUtf(buf))
        );
    }

    /** A kill feed row. The client stamps it with its own clock when it arrives. */
    public record KillFeedPayload(
        @Nullable Combatant killer,
        Combatant victim,
        float killerHealth,
        String weapon,
        float distance,
        int flags
    ) implements CustomPacketPayload {
        public static final Type<KillFeedPayload> TYPE = payloadType("kill_feed");
        public static final StreamCodec<FriendlyByteBuf, KillFeedPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBoolean(p.killer() != null);
                if (p.killer() != null) {
                    Combatant.CODEC.encode(buf, p.killer());
                }
                Combatant.CODEC.encode(buf, p.victim());
                buf.writeFloat(p.killerHealth());
                buf.writeUtf(p.weapon(), MAX_ID);
                buf.writeFloat(p.distance());
                buf.writeVarInt(p.flags());
            },
            buf -> new KillFeedPayload(
                buf.readBoolean() ? Combatant.CODEC.decode(buf) : null,
                Combatant.CODEC.decode(buf),
                buf.readFloat(),
                buf.readUtf(MAX_ID),
                buf.readFloat(),
                buf.readVarInt()
            )
        );

        @Override
        public Type<KillFeedPayload> type() {
            return TYPE;
        }
    }

    /**
     * What the victim sees on the death screen. {@code killer} is null for deaths without a living killer; then
     * {@code deathMessage} is the vanilla death message.
     */
    public record DeathRecapPayload(
        @Nullable Combatant killer,
        String deathMessage,
        float killerHealth,
        float killerMaxHealth,
        String weapon,
        float distance,
        float damageDealt,
        int flags
    ) implements CustomPacketPayload {
        public static final Type<DeathRecapPayload> TYPE = payloadType("death_recap");
        public static final StreamCodec<FriendlyByteBuf, DeathRecapPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBoolean(p.killer() != null);
                if (p.killer() != null) {
                    Combatant.CODEC.encode(buf, p.killer());
                }
                buf.writeUtf(p.deathMessage(), 1024);
                buf.writeFloat(p.killerHealth());
                buf.writeFloat(p.killerMaxHealth());
                buf.writeUtf(p.weapon(), MAX_ID);
                buf.writeFloat(p.distance());
                buf.writeFloat(p.damageDealt());
                buf.writeVarInt(p.flags());
            },
            buf -> new DeathRecapPayload(
                buf.readBoolean() ? Combatant.CODEC.decode(buf) : null,
                buf.readUtf(1024),
                buf.readFloat(),
                buf.readFloat(),
                buf.readUtf(MAX_ID),
                buf.readFloat(),
                buf.readFloat(),
                buf.readVarInt()
            )
        );

        @Override
        public Type<DeathRecapPayload> type() {
            return TYPE;
        }
    }

    /**
     * The receiving player's own mob kills by entity type. {@code replace}: the full list (on join); otherwise the
     * listed types' new totals (one entry per kill).
     */
    public record MobKillsPayload(boolean replace, Map<String, Integer> counts) implements CustomPacketPayload {
        public static final Type<MobKillsPayload> TYPE = payloadType("mob_kills");
        public static final StreamCodec<FriendlyByteBuf, MobKillsPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBoolean(p.replace());
                buf.writeVarInt(Math.min(p.counts().size(), MAX_MOB_TYPES));
                int written = 0;
                for (Map.Entry<String, Integer> entry : p.counts().entrySet()) {
                    if (written++ >= MAX_MOB_TYPES) {
                        break;
                    }
                    buf.writeUtf(entry.getKey(), MAX_ID);
                    buf.writeVarInt(entry.getValue());
                }
            },
            buf -> {
                boolean replace = buf.readBoolean();
                int size = readBoundedSize(buf, MAX_MOB_TYPES);
                Map<String, Integer> counts = new HashMap<>(size);
                for (int i = 0; i < size; i++) {
                    counts.put(buf.readUtf(MAX_ID), buf.readVarInt());
                }
                return new MobKillsPayload(replace, counts);
            }
        );

        @Override
        public Type<MobKillsPayload> type() {
            return TYPE;
        }
    }

    public record LeaderboardRow(UUID uuid, String name, int kills, int deaths, int mobKills) {
        static final StreamCodec<FriendlyByteBuf, LeaderboardRow> CODEC = StreamCodec.of(
            (buf, r) -> {
                buf.writeUUID(r.uuid());
                buf.writeUtf(r.name(), MAX_NAME);
                buf.writeVarInt(r.kills());
                buf.writeVarInt(r.deaths());
                buf.writeVarInt(r.mobKills());
            },
            buf -> new LeaderboardRow(buf.readUUID(), buf.readUtf(MAX_NAME), buf.readVarInt(), buf.readVarInt(), buf.readVarInt())
        );
    }

    /** The server's all-time table, sent in reply to {@link LeaderboardRequestPayload}. */
    public record LeaderboardPayload(List<LeaderboardRow> rows, int totalPlayers) implements CustomPacketPayload {
        public static final Type<LeaderboardPayload> TYPE = payloadType("leaderboard");
        public static final StreamCodec<FriendlyByteBuf, LeaderboardPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                int count = Math.min(p.rows().size(), MAX_LEADERBOARD_ROWS);
                buf.writeVarInt(count);
                for (int i = 0; i < count; i++) {
                    LeaderboardRow.CODEC.encode(buf, p.rows().get(i));
                }
                buf.writeVarInt(p.totalPlayers());
            },
            buf -> {
                int size = readBoundedSize(buf, MAX_LEADERBOARD_ROWS);
                List<LeaderboardRow> rows = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    rows.add(LeaderboardRow.CODEC.decode(buf));
                }
                return new LeaderboardPayload(rows, buf.readVarInt());
            }
        );

        @Override
        public Type<LeaderboardPayload> type() {
            return TYPE;
        }
    }

    /** Client asks for the leaderboard when the screen opens. Empty: the server answers with the whole table. */
    public record LeaderboardRequestPayload() implements CustomPacketPayload {
        public static final LeaderboardRequestPayload INSTANCE = new LeaderboardRequestPayload();
        public static final Type<LeaderboardRequestPayload> TYPE = payloadType("leaderboard_request");
        public static final StreamCodec<FriendlyByteBuf, LeaderboardRequestPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<LeaderboardRequestPayload> type() {
            return TYPE;
        }
    }

    /** A payload type with its codec, for the loaders' registration loops. */
    public record Entry<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type, StreamCodec<FriendlyByteBuf, T> codec) {
    }

    /** Server → client. */
    public static final List<Entry<?>> CLIENTBOUND = List.of(
        new Entry<>(KillFeedPayload.TYPE, KillFeedPayload.STREAM_CODEC),
        new Entry<>(DeathRecapPayload.TYPE, DeathRecapPayload.STREAM_CODEC),
        new Entry<>(MobKillsPayload.TYPE, MobKillsPayload.STREAM_CODEC),
        new Entry<>(LeaderboardPayload.TYPE, LeaderboardPayload.STREAM_CODEC)
    );

    /** Client → server. */
    public static final List<Entry<?>> SERVERBOUND = List.of(
        new Entry<>(LeaderboardRequestPayload.TYPE, LeaderboardRequestPayload.STREAM_CODEC)
    );
}
