package com.httpedor.rpgdamageoverhaul.damageproperties;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.DamageClassProperty;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;

import java.util.*;

public class ParentProperty extends DamageClassProperty {
    private static final Map<String, List<String>> children = new HashMap<>();

    public final String parentName;
    public ParentProperty(String parentName)
    {
        this.parentName = parentName;
    }

    @Override
    public void onRegistered(DamageClass dc) {
        super.onRegistered(dc);
        children.computeIfAbsent(parentName, k -> new LinkedList<>()).add(dc.name);
    }

    /** Cleared on unload: onRegistered appends, so reloading would otherwise list every child twice. */
    public static void clear()
    {
        children.clear();
    }

    public static Collection<String> getChildrenIds(String parentName)
    {
        return children.getOrDefault(parentName, List.of());
    }

    public static Collection<DamageClass> getChildren(String parentName)
    {
        var childrenIds = getChildrenIds(parentName);
        var result = new ArrayList<DamageClass>();
        for (var id : childrenIds)
        {
            var dc = RPGDamageOverhaulAPI.getDamageClass(id);
            if (dc != null)
                result.add(dc);
        }
        return result;
    }
    public static Collection<DamageClass> getChildren(DamageClass parent)
    {
        return getChildren(parent.name);
    }
}
