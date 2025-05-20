package com.httpedor.rpgdamageoverhaul.api;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class DamageHandler {

    public static Map<DamageSource, Double> applyDamageOverrides(LivingEntity entity, DamageSource source, float amount)
    {
        if (RPGDamageOverhaulAPI.isRPGDamageType(source.getType()))
            return null;

        Map<DamageClass, Double> overrides = RPGDamageOverhaulAPI.getDamageOverrides(source);
        if (overrides == null || overrides.isEmpty())
            return null;

        Map<DamageSource, Double> newDmgs = new HashMap<>();

        for (Map.Entry<DamageClass, Double> entry : overrides.entrySet()) {
            DamageClass dmgClass = entry.getKey();
            double dmg = entry.getValue();
            if (dmgClass == null)
                continue;

            RegistryEntry<DamageType> typeEntry = entity.getWorld().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(dmgClass.damageType);
            DamageSource newSource;
            if (source.getPosition() != null)
                newSource = new DamageSource(typeEntry, source.getPosition());
            else if (source.getSource() != null)
                newSource = new DamageSource(typeEntry, source.getSource(), source.getAttacker());
            else if (source.getAttacker() != null)
                newSource = new DamageSource(typeEntry, source.getAttacker());
            else
                newSource = new DamageSource(typeEntry);

            newDmgs.put(newSource, amount * dmg);
        }

        return newDmgs;
    }

    public static void executeOnHitEffects(DamageClass dc, LivingEntity target, DamageSource source, double damage)
    {
        executeOnHitEffect(new Identifier("rpgdamageoverhaul:" + dc.name), target, source, damage);
        for (Identifier onHitEffect : dc.onHitEffects)
            executeOnHitEffect(onHitEffect, target, source, damage);
    }

    public static void executeOnHitEffect(Identifier name, LivingEntity target, DamageSource source, double damage)
    {
        var callback = RPGDamageOverhaulAPI.onHitEffectCallbacks.getOrDefault(name, null);
        if (callback != null)
        {
            try{
                callback.accept(target, source, damage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
