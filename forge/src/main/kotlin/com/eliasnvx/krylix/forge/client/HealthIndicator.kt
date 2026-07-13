package com.eliasnvx.krylix.forge.client

import com.eliasnvx.krylix.core.HealthBarStyle
import com.eliasnvx.krylix.forge.config.KrylixConfig
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import kotlin.math.roundToInt

/**
 * Компактный индикатор здоровья над сущностью под прицелом (не под ограниченной дистанцией
 * взаимодействия, а расширенным raytrace — можно смотреть на моба через комнату).
 *
 * Билборд-позиционирование скопировано с точностью до констант из декомпилированного
 * EntityRenderer.renderNameTag — тот же translate/cameraOrientation/scale(-0.025,-0.025,0.025) и
 * тот же Font.drawInBatch(..., DisplayMode.SEE_THROUGH, ...), которым вавнильные неймтеги рендерятся
 * без искажений и без кастомных RenderType. Полоска HP нарисована символами блоков внутри текста —
 * это, а не отдельная текстурная геометрия, единственный способ получить "бар" без риска
 * не глядя сломать 3D-рендер (текстурный мини-аватар в мировом пространстве сюда сознательно не
 * добавлен — его нельзя было провалидировать без живого теста в игре).
 */
object HealthIndicator {
    private const val MAX_DISTANCE = 48.0
    private const val BAR_SEGMENTS = 10

    private var currentTarget: LivingEntity? = null

    /** Тумблер — сохраняется в конфиг, не сбрасывается при перезапуске */
    fun setEnabled(enabled: Boolean) {
        val config = KrylixConfig.get()
        config.healthIndicatorEnabled = enabled
        KrylixConfig.save()
    }

    fun isIndicatorEnabled(): Boolean = KrylixConfig.get().healthIndicatorEnabled

    /** Вызывается раз в клиентский тик (см. KrylixClient.onClientTick) — сам raytrace, не рендер */
    fun updateTarget() {
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player
        if (player == null) {
            currentTarget = null
            return
        }

        val level = player.level()
        val eye = player.eyePosition
        val look = player.lookAngle
        val end = eye.add(look.x * MAX_DISTANCE, look.y * MAX_DISTANCE, look.z * MAX_DISTANCE)
        val searchBox = player.boundingBox.expandTowards(look.scale(MAX_DISTANCE)).inflate(1.0)

        val hit = ProjectileUtil.getEntityHitResult(level, player, eye, end, searchBox) { candidate ->
            candidate is LivingEntity && candidate.isAlive && candidate !== player
        }
        currentTarget = hit?.entity as? LivingEntity
    }

    @SubscribeEvent
    fun onRenderLiving(event: RenderLivingEvent.Post<*, *>) {
        if (!KrylixConfig.get().healthIndicatorEnabled) return
        val target = currentTarget ?: return
        if (event.entity !== target) return
        render(event)
    }

    private fun render(event: RenderLivingEvent.Post<*, *>) {
        val entity = event.entity
        val minecraft = Minecraft.getInstance()
        val font = minecraft.font
        val poseStack = event.poseStack

        val maxHealth = entity.maxHealth.coerceAtLeast(1f)
        val pct = (entity.health / maxHealth).coerceIn(0f, 1f)
        val color = when {
            pct > 0.6f -> ChatFormatting.GREEN
            pct > 0.3f -> ChatFormatting.YELLOW
            else -> ChatFormatting.RED
        }
        val hpText: Component = Component.literal(barText(pct, entity.health.roundToInt(), maxHealth.roundToInt())).withStyle(color)
        val nameText: Component = entity.name

        // У сущностей с кастомным именем ваниль уже рисует свой неймтег, пока на них наведён
        // прицел (см. EntityRenderer.shouldShowName) — если продублировать ту же строку своим
        // рендером почти в той же точке мирового пространства, два независимых текстовых квада
        // начинают мерцать/подсвечивать друг друга (z-fighting). Поэтому для именованных сущностей
        // своё имя не рисуем вовсе, полагаясь на ванильный неймтег, и показываем только полоску HP.
        val showOwnName = !entity.hasCustomName()

        poseStack.pushPose()
        poseStack.translate(0.0, (entity.nameTagOffsetY + 0.3).toDouble(), 0.0)
        poseStack.mulPose(minecraft.entityRenderDispatcher.cameraOrientation())
        poseStack.scale(-0.025f, -0.025f, 0.025f)

        val matrix = poseStack.last().pose()
        val bgAlpha = (minecraft.options.getBackgroundOpacity(0.25f) * 255).toInt() shl 24
        val bufferSource = event.multiBufferSource
        val packedLight = event.packedLight

        if (showOwnName) {
            val nameWidth = font.width(nameText)
            font.drawInBatch(nameText, -nameWidth / 2f, 0f, 0xFFFFFF, false, matrix, bufferSource, Font.DisplayMode.SEE_THROUGH, bgAlpha, packedLight)
        }

        val hpY = if (showOwnName) 10f else 0f
        val hpWidth = font.width(hpText)
        font.drawInBatch(hpText, -hpWidth / 2f, hpY, 0xFFFFFF, false, matrix, bufferSource, Font.DisplayMode.SEE_THROUGH, bgAlpha, packedLight)

        poseStack.popPose()
    }

    /** Рендерит полоску HP по выбранному в конфиге стилю */
    private fun barText(pct: Float, hp: Int, maxHp: Int): String {
        val filled = (pct * BAR_SEGMENTS).roundToInt().coerceIn(0, BAR_SEGMENTS)
        val empty = BAR_SEGMENTS - filled
        return when (KrylixConfig.get().healthBarStyle) {
            HealthBarStyle.BLOCKS -> "${"█".repeat(filled)}${"░".repeat(empty)} $hp/$maxHp"
            HealthBarStyle.ASCII -> "[${"|".repeat(filled)}${".".repeat(empty)}] $hp/$maxHp"
            HealthBarStyle.DOTS -> "${"●".repeat(filled)}${"○".repeat(empty)} $hp/$maxHp"
            HealthBarStyle.NUMBER_ONLY -> "$hp/$maxHp HP"
        }
    }
}
