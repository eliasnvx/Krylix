package com.eliasnvx.krylix.forge.client

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation

/**
 * Единый источник текстур враждебных мобов, используемый и kill feed'ом (по display name,
 * т.к. на клиента прилетает уже локализованное имя жертвы/убийцы), и панелью статистики
 * убийств (по registry id сущности, который не зависит от локали).
 */
object MobTextures {
    private val texturesByEntityId: Map<String, ResourceLocation> = mapOf(
        "minecraft:zombie" to loc("entity/zombie/zombie"),
        "minecraft:skeleton" to loc("entity/skeleton/skeleton"),
        "minecraft:creeper" to loc("entity/creeper/creeper"),
        "minecraft:spider" to loc("entity/spider/spider"),
        "minecraft:cave_spider" to loc("entity/spider/cave_spider"),
        "minecraft:piglin" to loc("entity/piglin/piglin"),
        "minecraft:piglin_brute" to loc("entity/piglin/piglin_brute"),
        "minecraft:zombified_piglin" to loc("entity/piglin/zombified_piglin"),
        "minecraft:wither_skeleton" to loc("entity/skeleton/wither_skeleton"),
        "minecraft:stray" to loc("entity/skeleton/stray"),
        "minecraft:husk" to loc("entity/zombie/husk"),
        "minecraft:drowned" to loc("entity/zombie/drowned"),
        "minecraft:zombie_villager" to loc("entity/zombie_villager/zombie_villager"),
        "minecraft:blaze" to loc("entity/blaze"),
        "minecraft:ghast" to loc("entity/ghast/ghast"),
        "minecraft:witch" to loc("entity/witch"),
        "minecraft:pillager" to loc("entity/illager/pillager"),
        "minecraft:vindicator" to loc("entity/illager/vindicator"),
        "minecraft:evoker" to loc("entity/illager/evoker"),
        "minecraft:silverfish" to loc("entity/silverfish"),
        "minecraft:endermite" to loc("entity/endermite"),
        "minecraft:iron_golem" to loc("entity/iron_golem/iron_golem"),
        "minecraft:snow_golem" to loc("entity/snow_golem"),
        "minecraft:enderman" to loc("entity/enderman/enderman"),
        "minecraft:ravager" to loc("entity/illager/ravager"),
        "minecraft:warden" to loc("entity/warden/warden"),
        "minecraft:wither" to loc("entity/wither/wither"),
        // slime, magma_cube намеренно не включены: глаза/рот у них отдельные маленькие кубы
        // поверх тела со своим UV, а не часть текстуры "лица" — один прямоугольный blit не
        // соберёт их в осмысленную иконку (подтверждено декомпиляцией SlimeModel/LavaSlimeModel).
        // guardian, elder_guardian, shulker, phantom, vex, hoglin, zoglin, ender_dragon по той же
        // причине (сложная/составная геометрия либо кроп не читается как лицо при визуальной
        // проверке — см. crop_guardian/crop_shulker: просто чешуя/панцирь без узнаваемых черт)
        // не включены — эти мобы честно уходят в цветной fallback-квадрат вместо угаданной картинки.
    )

    /** Оверлей со светящимися глазами эндермена — рисуется вторым слоем поверх чёрной головы (см. blitMobFace) */
    private val endermanEyes = ResourceLocation("minecraft", "textures/entity/enderman/enderman_eyes.png")

    // Устаревшие/альтернативные display name, не совпадающие с registry id напрямую
    private val displayNameAliases: Map<String, String> = mapOf(
        "zombie pigman" to "minecraft:zombified_piglin",
        "magmacube" to "minecraft:magma_cube",
    )

    private fun loc(path: String) = ResourceLocation("minecraft", "textures/$path.png")

    fun byEntityId(entityId: String?): ResourceLocation? {
        if (entityId == null) return null
        return texturesByEntityId[entityId.lowercase()]
    }

    /** Угадывает registry id по локализованному display name (работает для vanilla английских имён) */
    fun guessEntityId(mobName: String?): String? {
        if (mobName == null) return null
        val normalized = mobName.lowercase()
        displayNameAliases[normalized]?.let { aliasId -> return aliasId.ifEmpty { null } }
        return "minecraft:" + normalized.replace(' ', '_')
    }

    /** Резолвит текстуру по локализованному display name моба (используется существующим kill feed'ом) */
    fun byDisplayName(mobName: String?): ResourceLocation? = byEntityId(guessEntityId(mobName))

    /**
     * Рендерит "лицо" моба — часть текстуры со специфичными для семейства мобов UV-координатами.
     * Все UV и реальные размеры текстур сверены с моделями из клиента (декомпилированы соответствующие
     * XxxModel.class) и провалидированы кроп-скриншотом самой текстуры, а не угаданы — угадывание уже
     * дважды давало неверный кусок картинки (голем показывал скин игрока, паук — кусок ноги вместо глаз).
     */
    fun blitMobFace(guiGraphics: GuiGraphics, texture: ResourceLocation, entityId: String?, x: Int, y: Int, size: Int) {
        when (entityId?.lowercase()) {
            "minecraft:zombie", "minecraft:husk", "minecraft:drowned" ->
                guiGraphics.blit(texture, x, y, size, size, 8.0f, 8.0f, 8, 8, 64, 64)
            "minecraft:enderman" -> {
                // Базовая текстура почти целиком чёрная — без второго слоя с глазами лицо
                // неотличимо от fallback-квадрата (проверено кропом, см. crop_enderman_base).
                // Оба blit используют одни и те же UV, т.к. enderman_eyes.png — это overlay
                // с прозрачным фоном и той же геометрией, что и enderman.png.
                RenderSystem.enableBlend()
                guiGraphics.blit(texture, x, y, size, size, 8.0f, 8.0f, 8, 8, 64, 32)
                guiGraphics.blit(endermanEyes, x, y, size, size, 8.0f, 8.0f, 8, 8, 64, 32)
                RenderSystem.disableBlend()
            }
            "minecraft:spider", "minecraft:cave_spider" ->
                // SpiderModel: head-куб 8x8x8 при texOffs(32,4) -> перёд лица в (40,12) на 64x32
                guiGraphics.blit(texture, x, y, size, size, 40.0f, 12.0f, 8, 8, 64, 32)
            "minecraft:pillager", "minecraft:vindicator", "minecraft:evoker", "minecraft:zombie_villager" ->
                // IllagerModel / ZombieVillagerModel: head-куб 8x10x8 при texOffs(0,0) -> перёд лица в (8,8), высота 10, на 64x64
                guiGraphics.blit(texture, x, y, size, size, 8.0f, 8.0f, 8, 10, 64, 64)
            "minecraft:witch" ->
                // VillagerModel (базовый класс WitchModel): head-куб 8x10x8 при texOffs(0,0) на 64x128
                guiGraphics.blit(texture, x, y, size, size, 8.0f, 8.0f, 8, 10, 64, 128)
            "minecraft:piglin", "minecraft:piglin_brute", "minecraft:zombified_piglin" ->
                // PiglinModel.addHead: куб 10x8x8 при texOffs(0,0) -> перёд лица в (8,8), ширина 10, на 64x64
                guiGraphics.blit(texture, x, y, size, size, 8.0f, 8.0f, 10, 8, 64, 64)
            "minecraft:ghast" ->
                // GhastModel: тело 16x16x16 при texOffs(0,0) -> перёд "лица" в (16,16) на 64x32
                guiGraphics.blit(texture, x, y, size, size, 16.0f, 16.0f, 16, 16, 64, 32)
            "minecraft:iron_golem" ->
                // IronGolemModel: head-куб 8x10x8 при texOffs(0,0) на текстуре 128x128 -> перёд лица в (8,8), высота 10
                guiGraphics.blit(texture, x, y, size, size, 8.0f, 8.0f, 8, 10, 128, 128)
            "minecraft:snow_golem" ->
                // SnowGolemModel: head-куб 8x8x8 при texOffs(0,0) на текстуре 64x64 -> перёд лица в (8,8)
                guiGraphics.blit(texture, x, y, size, size, 8.0f, 8.0f, 8, 8, 64, 64)
            "minecraft:ravager" ->
                // RavagerModel: head-куб 16x20x16 при texOffs(0,0) -> перёд лица в (16,16) на 128x128
                guiGraphics.blit(texture, x, y, size, size, 16.0f, 16.0f, 16, 20, 128, 128)
            "minecraft:warden" ->
                // WardenModel: head-куб 16x16x10 при texOffs(0,32) -> перёд лица в (10,42) на 128x128
                guiGraphics.blit(texture, x, y, size, size, 10.0f, 42.0f, 16, 16, 128, 128)
            "minecraft:wither" ->
                // WitherBossModel: center_head-куб 8x8x8 при texOffs(0,0), но текстура 64x64 (не 64x32,
                // как у стандартного humanoid-fallback) -> перёд лица в (8,8) на 64x64
                guiGraphics.blit(texture, x, y, size, size, 8.0f, 8.0f, 8, 8, 64, 64)
            else ->
                // Стандартный humanoid-стиль head-куб 8x8x8 при texOffs(0,0) на 64x32
                // (проверено на creeper/blaze/zombie-family; используется и для skeleton-семейства,
                // silverfish, endermite)
                guiGraphics.blit(texture, x, y, size, size, 8.0f, 8.0f, 8, 8, 64, 32)
        }
    }
}
