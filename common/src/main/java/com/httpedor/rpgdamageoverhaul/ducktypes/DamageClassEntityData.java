package com.httpedor.rpgdamageoverhaul.ducktypes;

import java.util.List;
import java.util.Map;

import com.httpedor.rpgdamageoverhaul.HurtCooldowns;
import com.httpedor.rpgdamageoverhaul.damageproperties.onhit.AttributeModifierOnHit;
import com.httpedor.rpgdamageoverhaul.damageproperties.onhit.HealingModifierOnHit;
import com.httpedor.rpgdamageoverhaul.damageproperties.onhit.ModifyDamageOnHit;
import com.httpedor.rpgdamageoverhaul.damageproperties.onhit.StackingOnHit;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Per-entity state for damage class properties, implemented on LivingEntity.
 * <p>
 * This used to live in static maps keyed by LivingEntity, which kept every entity ever hit alive for the rest of
 * the session and needed a removal hook to clean up. Hanging it off the entity means it is collected with the
 * entity, survives a dimension change or chunk unload exactly as long as the entity does, and can ride along in
 * the entity's own NBT where that makes sense -- so there is no removal hook to get wrong.
 */
public interface DamageClassEntityData {

    /**
     * Live stacks per damage class name, keyed by name rather than by {@link com.httpedor.rpgdamageoverhaul.api.DamageClass}
     * so the map stays valid across a datapack reload that rebuilds the class objects. Saved to NBT.
     */
    Map<String, List<StackingOnHit.StackInstance>> rpgdo$getDamageClassStacks();

    /**
     * Bookkeeping for the timed attribute modifiers this entity is under. Deliberately <b>not</b> saved: the
     * modifiers themselves go on via addTransientModifier, so vanilla drops them on save, and persisting their
     * expiry times would leave entries pointing at modifiers that no longer exist.
     */
    Map<ResourceLocation, AttributeModifierOnHit.ActiveModifier> rpgdo$getActiveAttributeModifiers();

    /** Heal modifications in effect. Not saved -- they last seconds and are keyed by the live property object. */
    Map<HealingModifierOnHit, HealingModifierOnHit.ActiveHealModifier> rpgdo$getActiveHealModifiers();

    /** Incoming-damage modifications in effect. Not saved, for the same reason. */
    Map<ModifyDamageOnHit, ModifyDamageOnHit.DamageModificationInstance> rpgdo$getDamageModifications();

    /**
     * Open invulnerability windows, one per (damage type, attacker, projectile) instead of vanilla's single
     * {@code invulnerableTime} timer -- see {@link com.httpedor.rpgdamageoverhaul.HurtCooldowns}. Not saved: a
     * window lasts half a second, and nothing should stay invulnerable across a relog.
     */
    Map<HurtCooldowns.Key, HurtCooldowns.Entry> rpgdo$getHurtCooldowns();

    /** Game time this entity stays artificially wet until. Not saved. */
    long rpgdo$getWetUntil();

    void rpgdo$setWetUntil(long gameTime);

    /**
     * Current per-damage-class absorption pools ("the actual amount of absorption hearts"), keyed by damage class
     * name. These behave like the vanilla Absorption effect but per class: the {@code X.absorption} attribute grants
     * them, {@code X.absorption_regen}/{@code X.absorption_regen_max} regenerate them, and taking class-{@code X}
     * damage drains them. Keyed by name so the map survives a datapack reload that rebuilds the class objects. Saved
     * to NBT, and synced to the owning player for the HUD.
     */
    Map<String, Float> rpgdo$getAbsorptionPools();

    /**
     * Last-seen value of each class' {@code absorption} attribute, so a change in it can be applied to the pool as a
     * delta -- exactly how the vanilla Absorption effect grants/removes hearts on apply/expire. Saved to NBT so a
     * relog doesn't look like the attribute was freshly granted and re-fill a drained pool.
     */
    Map<String, Float> rpgdo$getLastAbsorptionGranted();

    static DamageClassEntityData of(LivingEntity entity) {
        return (DamageClassEntityData) entity;
    }
}
