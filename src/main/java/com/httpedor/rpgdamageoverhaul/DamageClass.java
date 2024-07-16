package com.httpedor.rpgdamageoverhaul;


import com.google.gson.JsonElement;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

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

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DamageClass && ((DamageClass) obj).name.equals(name);
    }
}
