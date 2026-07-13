package com.eliasnvx.krylix.forge

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.saveddata.SavedData

/**
 * Хранит количество убийств враждебных мобов за весь мир (per-world, не per-dimension —
 * всегда читается/пишется через оверворлд текущего сервера).
 */
class MobKillStatsData : SavedData() {
    val kills: MutableMap<String, Int> = mutableMapOf()

    fun increment(entityId: String): Map<String, Int> {
        kills[entityId] = (kills[entityId] ?: 0) + 1
        setDirty()
        return kills
    }

    override fun save(tag: CompoundTag): CompoundTag {
        val killsTag = CompoundTag()
        kills.forEach { (id, count) -> killsTag.putInt(id, count) }
        tag.put("kills", killsTag)
        return tag
    }

    companion object {
        private const val ID = "krylix_mob_stats"

        private fun load(tag: CompoundTag): MobKillStatsData {
            val data = MobKillStatsData()
            val killsTag = tag.getCompound("kills")
            for (key in killsTag.allKeys) {
                data.kills[key] = killsTag.getInt(key)
            }
            return data
        }

        fun get(server: MinecraftServer): MobKillStatsData =
            server.overworld().dataStorage.computeIfAbsent(::load, ::MobKillStatsData, ID)
    }
}
