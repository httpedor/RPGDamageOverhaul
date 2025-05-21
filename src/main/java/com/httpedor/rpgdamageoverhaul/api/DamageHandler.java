package com.httpedor.rpgdamageoverhaul.api;


import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

public class DamageHandler {

    public static Map<DamageSource, Double> applyDamageOverrides(LivingEntity entity, DamageSource source, float amount)
    {
        if (RPGDamageOverhaulAPI.isRPGDamageType(source.type()))
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

            Holder<DamageType> typeEntry = entity.level().registryAccess().registry(Registries.DAMAGE_TYPE).get().getHolder(dmgClass.damageTypeKey).get();
            DamageSource newSource = new DamageSource(typeEntry, source.getDirectEntity(), source.getEntity(), source.getSourcePosition());

            newDmgs.put(newSource, amount * dmg);
        }

        return newDmgs;
    }

    public static void executeOnHitEffects(DamageClass dc, LivingEntity target, DamageSource source, double damage)
    {
        executeOnHitEffect(new ResourceLocation("rpgdamageoverhaul:" + dc.name), target, source, damage);
        for (ResourceLocation onHitEffect : dc.onHitEffects)
            executeOnHitEffect(onHitEffect, target, source, damage);
    }

    public static void executeOnHitEffect(ResourceLocation name, LivingEntity target, DamageSource source, double damage)
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
