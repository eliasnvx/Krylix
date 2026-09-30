package com.eliasnvx.krylix.client;

import com.eliasnvx.krylix.api.client.HealthProvider;
import com.eliasnvx.krylix.config.HealthBarStyle;
import com.eliasnvx.krylix.config.KrylixConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** The health plate over the entity under the crosshair. Client thread only. */
public final class HealthIndicator {
    private static final double MAX_DISTANCE = 48.0;
    /** Aiming leniency around hitboxes: far targets are small. */
    private static final double AIM_MARGIN = 0.25;
    /** Vanilla hides a sneaking entity's name from 32 blocks on. */
    private static final double DISCRETE_DISTANCE_SQ = 32.0 * 32.0;
    /** Height of one nametag line in world units (vanilla's 9 * 1.15 * 0.025). */
    private static final float LINE_HEIGHT = 0.25875f;
    private static final int BAR_SEGMENTS = 10;
    private static final int FILLED_COLOR = 0xFF5555;
    private static final int EMPTY_COLOR = 0x555555;
    private static final int TEXT_COLOR = 0xFFFFFF;

    private static final Identifier SPRITE_HEART_FULL = Identifier.withDefaultNamespace("hud/heart/full");
    private static final Identifier SPRITE_HEART_HALF = Identifier.withDefaultNamespace("hud/heart/half");
    private static final Identifier SPRITE_HEART_CONTAINER = Identifier.withDefaultNamespace("hud/heart/container");

    private static @Nullable LivingEntity currentTarget;
    /** Render state extracted for {@link #currentTarget} this frame: only that nametag gets the health plate. */
    private static @Nullable EntityRenderState targetState;

    /** The last health line, rebuilt only when the numbers or the style change (the plate is drawn every frame). */
    private record LineKey(HealthBarStyle style, int filled, boolean half, int total, int hp, int maxHp) {
    }

    private static @Nullable LineKey lineKey;
    /** Plates drawn since start, for the game tests (a target alone doesn't prove the plate was drawn). */
    private static int platesDrawn;
    private static @Nullable Component line;

    private HealthIndicator() {
    }

    public static void setEnabled(boolean enabled) {
        KrylixConfig.get().healthIndicatorEnabled = enabled;
        KrylixConfig.save();
        if (!enabled) {
            reset();
        }
    }

    public static boolean isIndicatorEnabled() {
        return KrylixConfig.get().healthIndicatorEnabled;
    }

    /** Forget the target (left the world, or plates switched off). */
    public static void reset() {
        currentTarget = null;
        targetState = null;
        lineKey = null;
        line = null;
    }

    public static @Nullable LivingEntity getCurrentTarget() {
        return currentTarget;
    }

    /** Once per client tick: find what the player is looking at. */
    public static void updateTarget() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !isIndicatorEnabled()) {
            currentTarget = null;
            targetState = null;
            return;
        }
        LivingEntity looked = findTarget(mc.player);
        if (looked == null && mc.crosshairPickEntity instanceof LivingEntity picked && picked.isAlive()) {
            looked = picked;
        }
        currentTarget = looked != null && looked.isAlive() && canShowPlate(looked, mc) ? looked : null;
        if (currentTarget == null) {
            targetState = null;
        }
    }

    /**
     * Mirrors vanilla's LivingEntityRenderer.shouldShowName and nametag distance: the plate must never reveal a name
     * the game hides (invisible, team rules, sneaking far away, the name tag distance attribute servers use, F1).
     */
    private static boolean canShowPlate(LivingEntity entity, Minecraft mc) {
        LocalPlayer viewer = mc.player;
        if (entity.isSpectator() || entity == mc.getCameraEntity() || mc.gui.hud.isHidden()) {
            return false;
        }
        if (entity instanceof ArmorStand && !entity.hasCustomName()) {
            return false; // decoration, not a mob
        }
        double distanceSq = entity.distanceToSqr(viewer);
        if (entity.isDiscrete() && distanceSq >= DISCRETE_DISTANCE_SQ) {
            return false;
        }
        if (distanceSq >= Mth.square(entity.getAttributeValue(Attributes.NAME_TAG_DISTANCE))) {
            return false;
        }
        boolean visible = !entity.isInvisibleTo(viewer);
        Team team = entity.getTeam();
        if (team == null) {
            return visible && !entity.isVehicle();
        }
        Team mine = viewer.getTeam();
        return switch (team.getNameTagVisibility()) {
            case ALWAYS -> visible;
            case NEVER -> false;
            case HIDE_FOR_OTHER_TEAMS -> mine == null ? visible : team.isAlliedTo(mine) && (team.canSeeFriendlyInvisibles() || visible);
            case HIDE_FOR_OWN_TEAM -> mine == null ? visible : !team.isAlliedTo(mine) && visible;
        };
    }

    /**
     * The nearest living entity along the view ray, up to {@link #MAX_DISTANCE} or the first block in the way. The
     * search box ends at that block, so looking at a wall doesn't scan every entity behind it.
     */
    private static @Nullable LivingEntity findTarget(LocalPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 far = eye.add(look.scale(MAX_DISTANCE));
        Vec3 blockHit = player.level().clip(new ClipContext(eye, far, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player)).getLocation();
        double reach = blockHit.distanceTo(eye);
        Vec3 end = eye.add(look.scale(reach));

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0);
        LivingEntity best = null;
        double bestDistanceSq = reach * reach;
        for (Entity entity : player.level().getEntities(player, searchBox,
            // Dying entities are skipped: one playing its death animation must not hide the mob behind it
            e -> e instanceof LivingEntity && e.isAlive() && e.isPickable() && !e.isSpectator())) {
            AABB box = entity.getBoundingBox().inflate(entity.getPickRadius() + AIM_MARGIN);
            if (box.contains(eye)) {
                return (LivingEntity) entity; // standing inside it
            }
            Optional<Vec3> hit = box.clip(eye, end);
            if (hit.isPresent()) {
                double distanceSq = eye.distanceToSqr(hit.get());
                if (distanceSq < bestDistanceSq) {
                    bestDistanceSq = distanceSq;
                    best = (LivingEntity) entity;
                }
            }
        }
        return best;
    }

    /**
     * During render-state extraction (every entity, every frame): returns true for the crosshair target, after making
     * sure its state has a name and a nametag anchor, so the loader lets its nametag render (the plate replaces it in
     * {@link #onSubmitNameDisplay}).
     */
    public static boolean applyNameplate(Entity entity, EntityRenderState state, float partialTick) {
        // The cheap identity test first: this runs for every rendered entity
        if (entity != currentTarget) {
            if (targetState == state) {
                targetState = null;
            }
            return false;
        }
        LivingEntity living = currentTarget;
        if (!living.isAlive() || !isIndicatorEnabled()) {
            return false;
        }
        targetState = state;
        if (state.nameTag == null) {
            state.nameTag = living.getDisplayName();
        }
        if (state.nameTagAttachment == null) {
            Vec3 attach = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getYRot(partialTick));
            state.nameTagAttachment = attach != null ? attach : new Vec3(0, entity.getBbHeight() + 0.5, 0);
        }
        return true;
    }

    /**
     * Draws the plate instead of the target's nametag. Returns false (vanilla draws) for every other entity.
     *
     * @param offset     vanilla's extra nametag offset (players with ears)
     * @param drawScore  whether to draw the below-name score line too (Fabric replaces the whole name display;
     *                   NeoForge has already drawn it and moved the pose up)
     */
    public static boolean onSubmitNameDisplay(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                                              CameraRenderState camera, int offset, boolean drawScore) {
        LivingEntity living = currentTarget;
        // Only the target's own nametag: every other name (players, named mobs) is left to vanilla
        if (state != targetState || living == null || !living.isAlive() || state.nameTagAttachment == null) {
            return false;
        }
        Vec3 attach = state.nameTagAttachment.add(0, 0.35, 0); // above horns, hats and heads
        poseStack.pushPose();
        if (drawScore && state.scoreText != null) {
            collector.submitNameTag(poseStack, state.nameTagAttachment, offset, state.scoreText, !state.isDiscrete, state.lightCoords, camera);
            poseStack.translate(0.0F, LINE_HEIGHT, 0.0F);
        }
        Component name = state.nameTag != null ? state.nameTag : living.getDisplayName();
        // Full bright: readable in caves and at night
        collector.submitNameTag(poseStack, attach, offset, name, !state.isDiscrete, LightCoordsUtil.FULL_BRIGHT, camera);
        collector.submitNameTag(poseStack, attach, offset + 10, healthLine(living), !state.isDiscrete, LightCoordsUtil.FULL_BRIGHT, camera);
        poseStack.popPose();
        platesDrawn++;
        return true;
    }

    public static int platesDrawn() {
        return platesDrawn;
    }

    private static Component healthLine(LivingEntity living) {
        HealthProvider.Health health = KrylixClientApiImpl.healthOf(living);
        float maxHp = Math.max(1f, health.max());
        float hp = Mth.clamp(health.current(), 0f, maxHp);
        HealthBarStyle style = KrylixConfig.get().healthBarStyle;

        int total;
        int filled;
        boolean half;
        if (style == HealthBarStyle.HEARTS) {
            total = Math.min(BAR_SEGMENTS, Math.max(1, Mth.ceil(maxHp / 2.0f)));
            float perHeart = maxHp / total;
            filled = (int) (hp / perHeart);
            half = filled < total && hp - filled * perHeart >= perHeart * 0.25f;
        } else {
            total = BAR_SEGMENTS;
            filled = hp > 0 ? Math.max(1, Math.round(hp / maxHp * total)) : 0; // a living target never shows empty
            half = false;
        }
        LineKey key = new LineKey(style, filled, half, total, Math.round(hp), Math.round(maxHp));
        if (!key.equals(lineKey)) {
            lineKey = key;
            line = buildLine(key);
        }
        return line;
    }

    private static Component buildLine(LineKey key) {
        MutableComponent line = Component.empty();
        String numbers = key.hp() + "/" + key.maxHp();
        int empty = key.total() - key.filled() - (key.half() ? 1 : 0);
        switch (key.style()) {
            case HEARTS -> {
                for (int i = 0; i < key.filled(); i++) {
                    line.append(sprite(SPRITE_HEART_FULL));
                }
                if (key.half()) {
                    line.append(sprite(SPRITE_HEART_HALF));
                }
                for (int i = 0; i < empty; i++) {
                    line.append(sprite(SPRITE_HEART_CONTAINER));
                }
                line.append(colored(" " + numbers, TEXT_COLOR));
            }
            case BLOCKS -> line.append(bar("■", "■", key)).append(colored(" " + numbers, TEXT_COLOR));
            case ASCII -> line.append(colored("[", TEXT_COLOR)).append(bar("|", ".", key))
                .append(colored("] " + numbers, TEXT_COLOR));
            case DOTS -> line.append(bar("●", "○", key)).append(colored(" " + numbers, TEXT_COLOR));
            case NUMBER_ONLY -> line.append(sprite(SPRITE_HEART_FULL)).append(colored(" " + numbers, TEXT_COLOR));
        }
        return line;
    }

    private static MutableComponent bar(String full, String empty, LineKey key) {
        return Component.empty()
            .append(colored(full.repeat(key.filled()), FILLED_COLOR))
            .append(colored(empty.repeat(key.total() - key.filled()), EMPTY_COLOR));
    }

    private static MutableComponent colored(String text, int rgb) {
        return Component.literal(text).withColor(rgb);
    }

    private static Component sprite(Identifier id) {
        return Component.object(new AtlasSprite(AtlasIds.GUI, id));
    }
}
