package com.eliasnvx.krylix.forge.client

import com.eliasnvx.krylix.forge.network.NetworkPackets.PlayerStatsSyncPacket.PlayerStatEntry

/**
 * Клиентский кэш PvP-рейтинга, приходящего с сервера через PlayerStatsSyncPacket
 * (per-world PlayerKillStatsData, см. серверный аналог).
 */
object PlayerStatsClient {
    private var entries: List<PlayerStatEntry> = emptyList()

    fun updateStats(newEntries: List<PlayerStatEntry>) {
        entries = newEntries
    }

    fun sortedByKills(): List<PlayerStatEntry> = entries.sortedByDescending { it.kills }

    fun sortedByMobKills(): List<PlayerStatEntry> = entries.sortedByDescending { it.mobKills }
}
