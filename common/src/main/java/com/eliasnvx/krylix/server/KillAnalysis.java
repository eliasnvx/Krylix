package com.eliasnvx.krylix.server;

import com.eliasnvx.krylix.addon.KrylixApiImpl;
import com.eliasnvx.krylix.api.event.KillCreditEvent;
import com.eliasnvx.krylix.network.KrylixPayloads.Combatant;
import com.eliasnvx.krylix.network.KrylixPayloads;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import org.jetbrains.annotations.Nullable;

/**
 * Everything the feed and the recap need to know about one death, worked out once on the server.
 *
 * @param killer   the living entity credited with the kill, or null for a plain environmental death
 * @param weapon   item id shown between the two names: the weapon used, or an item standing for the cause of death
 * @param distance killer-to-victim distance in blocks, or -1 without a killer
 */
public record KillAnalysis(@Nullable LivingEntity killer, LivingEntity victim, String weapon, float distance, int flags) {
    private static final double LONGSHOT_BLOCKS = 30.0;

    public static KillAnalysis of(LivingEntity victim, DamageSource source) {
        LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
        // Knocked off a cliff, into lava or into the void by someone: vanilla credits them ("doomed to fall by")
        LivingEntity killer = attacker != null ? attacker : victim.getKillCredit();
        // A tamed pet's kill is its owner's; the feed shows the pet's spawn egg as the weapon
        String petEgg = null;
        if (killer instanceof OwnableEntity pet && pet.getRootOwner() instanceof LivingEntity owner && killer != victim) {
            petEgg = SpawnEggItem.byId(killer.getType())
                .map(egg -> BuiltInRegistries.ITEM.getKey(egg.value()).toString())
                .orElse(null);
            killer = owner;
        }
        if (KrylixApiImpl.EVENTS.hasListeners(KillCreditEvent.class)) {
            killer = KrylixApiImpl.EVENTS.post(new KillCreditEvent(victim, source, killer)).killer();
        }
        if (killer == victim) {
            killer = null;
        }
        // Credited to someone who didn't deal the blow (or to no one, by an addon): the cause (or the pet) stands in
        // for the weapon. A plain environmental death has neither, and keeps whatever item the source carries.
        boolean credited = killer != attacker;

        ItemStack weaponStack = credited ? ItemStack.EMPTY : source.getWeaponItem();
        String weapon;
        if (weaponStack != null && !weaponStack.isEmpty()) {
            weapon = BuiltInRegistries.ITEM.getKey(weaponStack.getItem()).toString();
        } else if (petEgg != null) {
            weapon = petEgg;
        } else {
            weapon = causeItem(source);
        }

        float distance = killer != null ? (float) Math.sqrt(killer.distanceToSqr(victim)) : -1f;

        int flags = 0;
        if (source.is(DamageTypeTags.IS_MACE_SMASH)) {
            flags |= KrylixPayloads.FLAG_SMASH;
        }
        if (!credited && killer instanceof Player player && source.getDirectEntity() == player && isFallingCrit(player)) {
            flags |= KrylixPayloads.FLAG_CRITICAL;
        }
        // The shooter's own shot only: a pet's or a turret's kill credited to someone far away is no long shot
        if (!credited && source.getDirectEntity() instanceof Projectile && distance >= LONGSHOT_BLOCKS) {
            flags |= KrylixPayloads.FLAG_LONGSHOT;
        }
        return new KillAnalysis(killer, victim, weapon, distance, flags);
    }

    /** Vanilla's melee crit condition, checked at the moment of the killing blow. */
    private static boolean isFallingCrit(Player player) {
        return player.fallDistance > 0.0F && !player.onGround() && !player.onClimbable() && !player.isInWater()
            && !player.isPassenger() && !player.isSprinting();
    }

    /** An item that stands for how someone died, when no weapon was involved. */
    static String causeItem(DamageSource source) {
        if (source.is(DamageTypes.LAVA)) return "minecraft:lava_bucket";
        if (source.is(DamageTypeTags.IS_FIRE)) return "minecraft:fire_charge";
        if (source.is(DamageTypeTags.IS_FALL)) return "minecraft:feather";
        if (source.is(DamageTypeTags.IS_DROWNING) || source.is(DamageTypes.DRY_OUT)) return "minecraft:water_bucket";
        if (source.is(DamageTypeTags.IS_EXPLOSION)) return "minecraft:tnt";
        if (source.is(DamageTypeTags.IS_FREEZING)) return "minecraft:powder_snow_bucket";
        if (source.is(DamageTypeTags.IS_LIGHTNING)) return "minecraft:lightning_rod";
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.OUTSIDE_BORDER)) return "minecraft:ender_eye";
        if (source.is(DamageTypes.WITHER) || source.is(DamageTypes.WITHER_SKULL)) return "minecraft:wither_rose";
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) return "minecraft:potion";
        if (source.is(DamageTypes.STARVE)) return "minecraft:rotten_flesh";
        if (source.is(DamageTypes.CACTUS) || source.is(DamageTypes.SWEET_BERRY_BUSH)) return "minecraft:cactus";
        if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.CRAMMING)) return "minecraft:gravel";
        if (source.is(DamageTypes.FALLING_ANVIL)) return "minecraft:anvil";
        if (source.is(DamageTypes.FALLING_STALACTITE) || source.is(DamageTypes.STALAGMITE)) return "minecraft:pointed_dripstone";
        if (source.is(DamageTypes.SONIC_BOOM)) return "minecraft:echo_shard";
        if (source.is(DamageTypes.FIREWORKS)) return "minecraft:firework_rocket";
        if (source.is(DamageTypes.TRIDENT)) return "minecraft:trident";
        if (source.is(DamageTypeTags.IS_PROJECTILE)) return "minecraft:arrow";
        return "minecraft:air";
    }

    public static Combatant combatant(Entity entity) {
        return new Combatant(
            entity.getName().getString(),
            entity instanceof Player ? entity.getUUID() : null,
            EntityType.getKey(entity.getType()).toString()
        );
    }

    public @Nullable Combatant killerCombatant() {
        return killer != null ? combatant(killer) : null;
    }

    public Combatant victimCombatant() {
        return combatant(victim);
    }
}
