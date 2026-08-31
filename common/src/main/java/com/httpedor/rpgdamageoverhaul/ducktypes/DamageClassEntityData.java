package com.httpedor.rpgdamageoverhaul.ducktypes;

import java.util.List;
import java.util.Map;

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

    /** Game time this entity stays artificially wet until. Not saved. */
    long rpgdo$getWetUntil();

    void rpgdo$setWetUntil(long gameTime);

    static DamageClassEntityData of(LivingEntity entity) {
        return (DamageClassEntityData) entity;
    }
}
