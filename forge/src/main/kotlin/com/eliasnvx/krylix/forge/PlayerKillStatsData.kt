package com.eliasnvx.krylix.forge

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.saveddata.SavedData

data class PlayerStat(
    var kills: Int = 0,
    var deaths: Int = 0,
    var mobKills: Int = 0,
    var lastName: String = "",
)

/**
 * Рейтинг игроков по PvP-убийствам и личным убийствам мобов за весь мир (per-world, аналогично
 * MobKillStatsData — тот считает "какого моба убивали чаще всего", а этот "кто из игроков убил
 * больше мобов"). Ключ — UUID игрока строкой (стабилен в отличие от ника).
 */
class PlayerKillStatsData : SavedData() {
    val stats: MutableMap<String, PlayerStat> = mutableMapOf()

    fun recordKill(killerUuid: String, killerName: String) {
        val stat = stats.getOrPut(killerUuid) { PlayerStat() }
        stat.kills++
        stat.lastName = killerName
        setDirty()
    }

    fun recordDeath(victimUuid: String, victimName: String) {
        val stat = stats.getOrPut(victimUuid) { PlayerStat() }
        stat.deaths++
        stat.lastName = victimName
        setDirty()
    }

    fun recordMobKill(killerUuid: String, killerName: String) {
        val stat = stats.getOrPut(killerUuid) { PlayerStat() }
        stat.mobKills++
        stat.lastName = killerName
        setDirty()
    }

    override fun save(tag: CompoundTag): CompoundTag {
        val statsTag = CompoundTag()
        stats.forEach { (uuid, stat) ->
            val entryTag = CompoundTag()
            entryTag.putInt("kills", stat.kills)
            entryTag.putInt("deaths", stat.deaths)
            entryTag.putInt("mobKills", stat.mobKills)
            entryTag.putString("name", stat.lastName)
            statsTag.put(uuid, entryTag)
        }
        tag.put("stats", statsTag)
        return tag
    }

    companion object {
        private const val ID = "krylix_player_stats"

        private fun load(tag: CompoundTag): PlayerKillStatsData {
            val data = PlayerKillStatsData()
            val statsTag = tag.getCompound("stats")
            for (uuid in statsTag.allKeys) {
                val entryTag = statsTag.getCompound(uuid)
                data.stats[uuid] = PlayerStat(
                    kills = entryTag.getInt("kills"),
                    deaths = entryTag.getInt("deaths"),
                    mobKills = entryTag.getInt("mobKills"),
                    lastName = entryTag.getString("name"),
                )
            }
            return data
        }

        fun get(server: MinecraftServer): PlayerKillStatsData =
            server.overworld().dataStorage.computeIfAbsent(::load, ::PlayerKillStatsData, ID)
    }
}
