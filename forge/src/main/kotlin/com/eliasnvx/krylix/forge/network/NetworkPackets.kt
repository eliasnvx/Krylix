package com.eliasnvx.krylix.forge.network

import com.eliasnvx.krylix.Krylix
import com.eliasnvx.krylix.forge.PlayerStat
import com.eliasnvx.krylix.model.KillEntry
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.network.NetworkDirection
import net.minecraftforge.network.NetworkEvent
import net.minecraftforge.network.NetworkRegistry
import net.minecraftforge.network.PacketDistributor
import net.minecraftforge.network.simple.SimpleChannel
import java.util.function.Supplier

/**
 * Network packets для синхронизации kill feed между сервером и клиентами
 */
object NetworkPackets {
    private const val PROTOCOL_VERSION = "1"

    val CHANNEL: SimpleChannel = NetworkRegistry.newSimpleChannel(
        ResourceLocation(Krylix.MOD_ID, "main"),
        { PROTOCOL_VERSION },
        { PROTOCOL_VERSION == it },
        { PROTOCOL_VERSION == it }
    )

    init {
        CHANNEL.messageBuilder(KillNotificationPacket::class.java, 0)
            .encoder(KillNotificationPacket::encode)
            .decoder(KillNotificationPacket::decode)
            .consumerMainThread { msg, ctx ->
                msg.handle(ctx)
                true
            }
            .add()

        CHANNEL.messageBuilder(MobStatsSyncPacket::class.java, 1)
            .encoder(MobStatsSyncPacket::encode)
            .decoder(MobStatsSyncPacket::decode)
            .consumerMainThread { msg, ctx ->
                msg.handle(ctx)
                true
            }
            .add()

        CHANNEL.messageBuilder(PlayerStatsSyncPacket::class.java, 2)
            .encoder(PlayerStatsSyncPacket::encode)
            .decoder(PlayerStatsSyncPacket::decode)
            .consumerMainThread { msg, ctx ->
                msg.handle(ctx)
                true
            }
            .add()
    }

    /**
     * Отправляет уведомление об убийстве всем игрокам на сервере
     */
    fun sendKillNotificationToAll(killEntry: KillEntry) {
        val packet = KillNotificationPacket(killEntry)
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet)
        Krylix.LOGGER.debug("Sent kill notification to all clients: $killEntry")
    }

    /**
     * Отправляет уведомление конкретному игроку
     */
    fun sendKillNotificationToPlayer(player: ServerPlayer, killEntry: KillEntry) {
        val packet = KillNotificationPacket(killEntry)
        CHANNEL.send(PacketDistributor.PLAYER.with { player }, packet)
        Krylix.LOGGER.debug("Sent kill notification to ${player.name.string}: $killEntry")
    }

    /**
     * Отправляет уведомление об убийстве только перечисленным игрокам (напр. в пределах одного измерения)
     */
    fun sendKillNotificationToPlayers(players: Collection<ServerPlayer>, killEntry: KillEntry) {
        val packet = KillNotificationPacket(killEntry)
        players.forEach { player -> CHANNEL.send(PacketDistributor.PLAYER.with { player }, packet) }
        Krylix.LOGGER.debug("Sent kill notification to ${players.size} players: $killEntry")
    }

    /**
     * Отправляет снапшот статистики убийств мобов конкретному игроку
     */
    fun sendMobStatsSync(player: ServerPlayer, kills: Map<String, Int>) {
        CHANNEL.send(PacketDistributor.PLAYER.with { player }, MobStatsSyncPacket(kills))
        Krylix.LOGGER.debug("Sent mob stats sync to ${player.name.string}: $kills")
    }

    /**
     * Отправляет снапшот PvP-рейтинга конкретному игроку (напр. при входе)
     */
    fun sendPlayerStatsSync(player: ServerPlayer, stats: Map<String, PlayerStat>) {
        CHANNEL.send(PacketDistributor.PLAYER.with { player }, PlayerStatsSyncPacket(toEntries(stats)))
    }

    /**
     * Рассылает снапшот PvP-рейтинга всем игрокам (после каждого PvP-килла) — рейтинг это
     * общесерверная сводка, поэтому рассылается всем независимо от restrictBroadcastToSameDimension
     */
    fun broadcastPlayerStatsSync(stats: Map<String, PlayerStat>) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), PlayerStatsSyncPacket(toEntries(stats)))
    }

    private fun toEntries(stats: Map<String, PlayerStat>): List<PlayerStatsSyncPacket.PlayerStatEntry> =
        stats.map { (uuid, stat) -> PlayerStatsSyncPacket.PlayerStatEntry(uuid, stat.lastName, stat.kills, stat.deaths, stat.mobKills) }

    /**
     * Packet для уведомления об убийстве
     */
    data class KillNotificationPacket(
        val killerName: String?,
        val victimName: String,
        val weaponName: String,
        val distance: Double?,
        val timestamp: Long,
        val isHeadshot: Boolean
    ) {

        constructor(killEntry: KillEntry) : this(
            killEntry.killerName,
            killEntry.victimName,
            killEntry.weaponName,
            killEntry.distance,
            killEntry.timestamp,
            killEntry.isHeadshot
        )

        fun encode(buf: FriendlyByteBuf) {
            buf.writeUtf(killerName ?: "")
            buf.writeUtf(victimName)
            buf.writeUtf(weaponName)
            buf.writeDouble(distance ?: 0.0)
            buf.writeLong(timestamp)
            buf.writeBoolean(isHeadshot)
        }

        fun handle(ctx: Supplier<NetworkEvent.Context>) {
            ctx.get().enqueueWork {
                // Обрабатываем только на клиенте
                if (ctx.get().direction == NetworkDirection.PLAY_TO_CLIENT) {
                    handleOnClient()
                }
            }
            ctx.get().packetHandled = true
        }

        @OnlyIn(Dist.CLIENT)
        private fun handleOnClient() {
            val killEntry = KillEntry(
                killerName = killerName,
                victimName = victimName,
                weaponName = weaponName,
                distance = distance,
                timestamp = timestamp,
                isHeadshot = isHeadshot
            )

            // Добавляем в client-side HUD только на клиенте
            com.eliasnvx.krylix.forge.client.KillFeedHud.addNotification(killEntry)

            Krylix.LOGGER.debug("Received kill notification on client: $killEntry")
        }

        companion object {
            fun decode(buf: FriendlyByteBuf): KillNotificationPacket {
                val killerName = buf.readUtf().takeIf { it.isNotEmpty() }
                val victimName = buf.readUtf()
                val weaponName = buf.readUtf()
                val distance = buf.readDouble().takeIf { it != 0.0 }
                val timestamp = buf.readLong()
                val isHeadshot = buf.readBoolean()

                return KillNotificationPacket(
                    killerName,
                    victimName,
                    weaponName,
                    distance,
                    timestamp,
                    isHeadshot
                )
            }
        }
    }

    /**
     * Packet со снапшотом статистики убийств мобов (registry id -> количество)
     */
    data class MobStatsSyncPacket(val kills: Map<String, Int>) {

        fun encode(buf: FriendlyByteBuf) {
            buf.writeVarInt(kills.size)
            kills.forEach { (id, count) ->
                buf.writeUtf(id)
                buf.writeVarInt(count)
            }
        }

        fun handle(ctx: Supplier<NetworkEvent.Context>) {
            ctx.get().enqueueWork {
                if (ctx.get().direction == NetworkDirection.PLAY_TO_CLIENT) {
                    handleOnClient()
                }
            }
            ctx.get().packetHandled = true
        }

        @OnlyIn(Dist.CLIENT)
        private fun handleOnClient() {
            com.eliasnvx.krylix.forge.client.MobStatsClient.updateStats(kills)
        }

        companion object {
            fun decode(buf: FriendlyByteBuf): MobStatsSyncPacket {
                val size = buf.readVarInt()
                val kills = buildMap {
                    repeat(size) {
                        val id = buf.readUtf()
                        val count = buf.readVarInt()
                        put(id, count)
                    }
                }
                return MobStatsSyncPacket(kills)
            }
        }
    }

    /**
     * Packet со снапшотом PvP-рейтинга игроков
     */
    data class PlayerStatsSyncPacket(val entries: List<PlayerStatEntry>) {

        data class PlayerStatEntry(val uuid: String, val name: String, val kills: Int, val deaths: Int, val mobKills: Int)

        fun encode(buf: FriendlyByteBuf) {
            buf.writeVarInt(entries.size)
            entries.forEach { entry ->
                buf.writeUtf(entry.uuid)
                buf.writeUtf(entry.name)
                buf.writeVarInt(entry.kills)
                buf.writeVarInt(entry.deaths)
                buf.writeVarInt(entry.mobKills)
            }
        }

        fun handle(ctx: Supplier<NetworkEvent.Context>) {
            ctx.get().enqueueWork {
                if (ctx.get().direction == NetworkDirection.PLAY_TO_CLIENT) {
                    handleOnClient()
                }
            }
            ctx.get().packetHandled = true
        }

        @OnlyIn(Dist.CLIENT)
        private fun handleOnClient() {
            com.eliasnvx.krylix.forge.client.PlayerStatsClient.updateStats(entries)
        }

        companion object {
            fun decode(buf: FriendlyByteBuf): PlayerStatsSyncPacket {
                val size = buf.readVarInt()
                val entries = buildList {
                    repeat(size) {
                        val uuid = buf.readUtf()
                        val name = buf.readUtf()
                        val kills = buf.readVarInt()
                        val deaths = buf.readVarInt()
                        val mobKills = buf.readVarInt()
                        add(PlayerStatEntry(uuid, name, kills, deaths, mobKills))
                    }
                }
                return PlayerStatsSyncPacket(entries)
            }
        }
    }
}
