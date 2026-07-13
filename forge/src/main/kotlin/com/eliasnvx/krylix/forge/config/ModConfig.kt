package com.eliasnvx.krylix.forge.config

import com.eliasnvx.krylix.core.KrylixConfigData
import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.ConfigData
import me.shedaniel.autoconfig.annotation.Config
import me.shedaniel.autoconfig.annotation.ConfigEntry

@Config(name = "krylix")
class ModConfig : ConfigData {
    // documentation: https://shedaniel.gitbook.io/cloth-config/auto-config/creating-a-config-class

    @ConfigEntry.Gui.CollapsibleObject
    val config = KrylixConfigData()
}

/** Единая точка доступа к текущим значениям конфига (сервер и клиент читают одно и то же) */
object KrylixConfig {
    fun get(): KrylixConfigData = AutoConfig.getConfigHolder(ModConfig::class.java).config.config

    /** Сохраняет текущее состояние на диск — вызывать после ручного изменения полей [get] */
    fun save() = AutoConfig.getConfigHolder(ModConfig::class.java).save()
}
