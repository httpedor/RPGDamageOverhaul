package com.httpedor.rpgdamageoverhaul.damageproperties;

import java.util.*;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.DamageClassProperty;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

public class IsDamageTypeProperty extends DamageClassProperty {
    private static final Map<ResourceLocation, Set<ResourceLocation>> mappedDamageTypes = new HashMap<>();
    Set<ResourceLocation> damageTypes;

    public IsDamageTypeProperty(Collection<ResourceLocation> damageTypes) {
        this.damageTypes = new HashSet<>(damageTypes);
    }

    @Override
    public void onRegistered(DamageClass dc) {
        mappedDamageTypes.computeIfAbsent(ResourceLocation.fromNamespaceAndPath("rpgdamageoverhaul", dc.name), k -> new HashSet<>()).addAll(damageTypes);

    }

    /**
     * Cleared on unload: onRegistered accumulates with addAll, so without this a damage type keeps aliasing to a
     * damage class from a previous world, and HolderMixin goes on reporting it as that class.
     */
    public static void clear() {
        mappedDamageTypes.clear();
    }

    public static Collection<ResourceLocation> getDamageTypes(ResourceLocation damageType) {
        if (mappedDamageTypes.containsKey(damageType))
            return mappedDamageTypes.get(damageType);
        return new ArrayList<>();
    }
    public static Collection<ResourceLocation> getDamageTypes(ResourceKey<DamageType> damageType) {
        return getDamageTypes(damageType.location());
    }

    @Override
    public DamageClassProperty mergeWith(DamageClassProperty other) {
        Set<ResourceLocation> mergedDamageTypes = new HashSet<>(this.damageTypes);
        if (other instanceof IsDamageTypeProperty otherIsDamageType) {
            mergedDamageTypes.addAll(otherIsDamageType.damageTypes);
        }
        return new IsDamageTypeProperty(mergedDamageTypes);
    }
}
