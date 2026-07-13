package com.example.modid.forge.network

import com.example.modid.krylix.Krylix
import com.example.modid.krylix.model.KillEntry
import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
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
            com.example.modid.forge.client.KillFeedHud.addNotification(killEntry)
            
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
}
