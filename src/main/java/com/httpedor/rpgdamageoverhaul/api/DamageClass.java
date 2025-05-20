package com.httpedor.rpgdamageoverhaul.api;


import com.google.gson.JsonElement;
import com.httpedor.rpgdamageoverhaul.ducktypes.DCDamageSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DamageClass {

    public final String name;
    public final EntityAttribute dmgAttribute;
    public final EntityAttribute armorAttribute;
    public final EntityAttribute absorptionAttribute;
    public final EntityAttribute resistanceAttribute;
    public final RegistryKey<DamageType> damageType;
    RegistryEntry<DamageType> damageTypeEntry;
    public final Set<Identifier> onHitEffects;
    public Map<String, JsonElement> properties;
    public final String parentName;

    DamageClass(String name,
                EntityAttribute dmgAttribute,
                EntityAttribute armorAttribute,
                EntityAttribute absorptionAttribute,
                EntityAttribute resistanceAttribute,
                RegistryKey<DamageType> damageType,
                String parentName
    )
    {
        this.name = name;
        this.dmgAttribute = dmgAttribute;
        this.armorAttribute = armorAttribute;
        this.absorptionAttribute = absorptionAttribute;
        this.resistanceAttribute = resistanceAttribute;
        this.damageType = damageType;
        this.parentName = parentName;
        this.onHitEffects = new HashSet<>();
        this.properties = new HashMap<>();
    }

    public void addOnHitEffect(Identifier effect)
    {
        onHitEffects.add(effect);
    }
    public void removeOnHitEffect(Identifier effect)
    {
        onHitEffects.remove(effect);
    }

    public RegistryEntry<DamageType> getDamageTypeEntry()
    {
        return damageTypeEntry;
    }

    public boolean isChildOf(String parentName)
    {
        boolean isChild = false;
        DamageClass parent = RPGDamageOverhaulAPI.getDamageClass(parentName);
        while (parent != null)
        {
            if (parent.name.equals(parentName))
            {
                isChild = true;
                break;
            }
            parent = RPGDamageOverhaulAPI.getDamageClass(parent.parentName);
        }
        return isChild;
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
    public DamageSource createDamageSource(Vec3d position)
    {
        return createDamageSource(position, true);
    }
    public DamageSource createDamageSource()
    {
        return createDamageSource(true);
    }

    public DamageSource createDamageSource(Entity attacker, boolean triggerOnHitEffects)
    {
        DamageSource ret = new DamageSource(damageTypeEntry, attacker);
        ((DCDamageSource)ret).setTriggerOnHitEffects(triggerOnHitEffects);
        return ret;
    }
    public DamageSource createDamageSource(Entity attacker, Entity source, boolean triggerOnHitEffects)
    {
        DamageSource ret = new DamageSource(damageTypeEntry, attacker, source);
        ((DCDamageSource)ret).setTriggerOnHitEffects(triggerOnHitEffects);
        return ret;
    }
    public DamageSource createDamageSource(Vec3d position, boolean triggerOnHitEffects)
    {
        DamageSource ret = new DamageSource(damageTypeEntry, position);
        ((DCDamageSource)ret).setTriggerOnHitEffects(triggerOnHitEffects);
        return ret;
    }
    public DamageSource createDamageSource(boolean triggerOnHitEffects)
    {
        DamageSource ret = new DamageSource(damageTypeEntry);
        ((DCDamageSource)ret).setTriggerOnHitEffects(triggerOnHitEffects);
        return ret;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DamageClass && ((DamageClass) obj).name.equals(name);
    }
}
