package com.httpedor.rpgdamageoverhaul.damageproperties;

import java.util.*;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.DamageClassProperty;

import net.minecraft.resources.ResourceLocation;

public class HasTagsProperty extends DamageClassProperty {
    private static final Map<ResourceLocation, Set<ResourceLocation>> mappedTags = new HashMap<>();
    private ResourceLocation[] tags;

    public HasTagsProperty(ResourceLocation... tags) {
        this.tags = tags;
    }

    /**
     * Cleared on unload: onRegistered uses computeIfAbsent, so a stale entry would make the new pack's tags for
     * that damage class be silently dropped.
     */
    public static void clear() {
        mappedTags.clear();
    }

    public static Set<ResourceLocation> getTags(ResourceLocation damageClass) {
        if (mappedTags.containsKey(damageClass))
            return mappedTags.get(damageClass);
        return Set.of();
    }

    @Override
    public DamageClassProperty mergeWith(DamageClassProperty other) {
        Set<ResourceLocation> mergedTags = new HashSet<>(List.of(this.tags));
        if (other instanceof HasTagsProperty otherHasTags) {
            mergedTags.addAll(Arrays.asList(otherHasTags.tags));
        }
        tags = mergedTags.toArray(new ResourceLocation[0]);
        return this;
    }

    @Override
    public void onRegistered(DamageClass dc) {
        mappedTags.computeIfAbsent(ResourceLocation.fromNamespaceAndPath("rpgdamageoverhaul", dc.name), k -> Set.of(tags));
    }
}
