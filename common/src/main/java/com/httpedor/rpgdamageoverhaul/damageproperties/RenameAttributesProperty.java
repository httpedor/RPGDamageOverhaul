package com.httpedor.rpgdamageoverhaul.damageproperties;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.DamageClassProperty;
import net.minecraft.resources.ResourceLocation;
import oshi.util.tuples.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RenameAttributesProperty extends DamageClassProperty {
    private static final Map<ResourceLocation, Pair<DamageClass, DamageClass.DCAttribute>> nameMap = new HashMap<>();
    public final Map<ResourceLocation, DamageClass.DCAttribute> attributes;

    public RenameAttributesProperty(Map<ResourceLocation, DamageClass.DCAttribute> attributes)
    {
        this.attributes = attributes;
    }

    @Override
    public void onRegistered(DamageClass dc) {
        super.onRegistered(dc);
        for (var attr : attributes.entrySet()) {
            nameMap.put(attr.getKey(), new Pair<>(dc, attr.getValue()));
        }
    }

    /** Renames from a previous world would otherwise keep pointing at damage classes that no longer exist. */
    public static void clear()
    {
        nameMap.clear();
    }

    public static Pair<DamageClass, DamageClass.DCAttribute> getDcForAttribute(ResourceLocation attr)
    {
        return nameMap.get(attr);
    }
}
