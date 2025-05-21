package com.httpedor.rpgdamageoverhaul.api;


import com.google.gson.JsonElement;
import com.httpedor.rpgdamageoverhaul.ducktypes.DCDamageSource;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DamageClass {

    public final String name;
    public final Attribute dmgAttribute;
    public final Attribute armorAttribute;
    public final Attribute absorptionAttribute;
    public final Attribute resistanceAttribute;
    public final ResourceKey<DamageType> damageTypeKey;
    public DamageType damageType;
    public final Set<ResourceLocation> onHitEffects;
    public Map<String, JsonElement> properties;
    public final String parentName;

    DamageClass(String name,
                Attribute dmgAttribute,
                Attribute armorAttribute,
                Attribute absorptionAttribute,
                Attribute resistanceAttribute,
                ResourceKey<DamageType> damageType,
                String parentName
    )
    {
        this.name = name;
        this.dmgAttribute = dmgAttribute;
        this.armorAttribute = armorAttribute;
        this.absorptionAttribute = absorptionAttribute;
        this.resistanceAttribute = resistanceAttribute;
        this.damageTypeKey = damageType;
        this.parentName = parentName;
        this.onHitEffects = new HashSet<>();
        this.properties = new HashMap<>();
    }

    public void addOnHitEffect(ResourceLocation effect)
    {
        onHitEffects.add(effect);
    }
    public void removeOnHitEffect(ResourceLocation effect)
    {
        onHitEffects.remove(effect);
    }

    public Holder<DamageType> getDamageType()
    {
        return Holder.direct(damageType);
    }

    public boolean isChildOf(String parentName)
    {
        DamageClass parent = RPGDamageOverhaulAPI.getDamageClass(this.parentName);
        while (parent != null)
        {
            if (parent.name.equals(parentName))
            {
                return true;
            }
            parent = RPGDamageOverhaulAPI.getDamageClass(parent.parentName);
        }
        return false;
    }
    public boolean isChildOf(DamageClass parent)
    {
        return isChildOf(parent.name);
    }

    public DamageSource createDamageSource(Entity attacker)
    {
        return createDamageSource(attacker, true);
    }
    public DamageSource createDamageSource(Entity attacker, Entity source)
    {
        return createDamageSource(attacker, source, true);
    }
    public DamageSource createDamageSource(Vec3 position)
    {
        return createDamageSource(position, true);
    }
    public DamageSource createDamageSource()
    {
        return createDamageSource(true);
    }

    public DamageSource createDamageSource(Entity attacker, boolean triggerOnHitEffects)
    {
        DamageSource ret = new DamageSource(getDamageType(), attacker);
        ((DCDamageSource)ret).setTriggerOnHitEffects(triggerOnHitEffects);
        return ret;
    }
    public DamageSource createDamageSource(Entity attacker, Entity source, boolean triggerOnHitEffects)
    {
        DamageSource ret = new DamageSource(getDamageType(), attacker, source);
        ((DCDamageSource)ret).setTriggerOnHitEffects(triggerOnHitEffects);
        return ret;
    }
    public DamageSource createDamageSource(Vec3 position, boolean triggerOnHitEffects)
    {
        DamageSource ret = new DamageSource(getDamageType(), position);
        ((DCDamageSource)ret).setTriggerOnHitEffects(triggerOnHitEffects);
        return ret;
    }
    public DamageSource createDamageSource(boolean triggerOnHitEffects)
    {
        DamageSource ret = new DamageSource(getDamageType());
        ((DCDamageSource)ret).setTriggerOnHitEffects(triggerOnHitEffects);
        return ret;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DamageClass && ((DamageClass) obj).name.equals(name);
    }
}
