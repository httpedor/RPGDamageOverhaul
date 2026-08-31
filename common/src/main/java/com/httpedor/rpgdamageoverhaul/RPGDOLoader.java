package com.httpedor.rpgdamageoverhaul;

import java.util.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.DamageClassProperty;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;

public class RPGDOLoader {
    public static record DamageClassConfig(int priority, JsonObject originalJson) { }


    private static final Map<String, List<DamageClassConfig>> damageClasses = new HashMap<>();

    public static void clear()
    {
        damageClasses.clear();
    }

    public static void addDamageClassConfig(String name, JsonObject obj)
    {
        // Only the raw json is kept here. Properties are parsed in buildDamageClasses, once every damage class'
        // attributes exist -- see the comment there.
        int priority = obj.has("priority") ? obj.get("priority").getAsInt() : 0;
        damageClasses.computeIfAbsent(name, k -> new ArrayList<>()).add(new DamageClassConfig(priority, obj));
    }
    public static void buildDamageClasses(RegistryAccess ra)
    {
        // Register every damage class' attributes before parsing a single property. Property builders resolve
        // attributes by name (attribute_modifiers does, for one), so a class referencing a sibling's attribute
        // fails with "Unknown attribute: rpgdamageoverhaul:<class>.<attr>" and gets silently dropped if the
        // sibling happens to be registered later -- which made it depend on hash order and on whether the
        // attribute happened to survive in the registry from an earlier world.
        List<Holder<Attribute>> attributes = new ArrayList<>();
        for (var dcName : damageClasses.keySet())
            attributes.addAll(RPGDamageOverhaulAPI.registerDamageClassAttributes(dcName).values());
        RPGDamageOverhaulAPI.injectAttributesIntoEntityTypes(attributes);

        for (var entry : damageClasses.entrySet())
        {
            String dcName = entry.getKey();
            var dcConfigs = entry.getValue();
            Map<String, DamageClassProperty> mergedProperties = new HashMap<>();
            // Sort dcConfigs by priority
            dcConfigs.sort(Comparator.comparingInt(DamageClassConfig::priority));
            for (var dcConfig : dcConfigs)
            {
                for (var propEntry : dcConfig.originalJson.entrySet())
                {
                    String propName = propEntry.getKey();
                    if (propName.equals("priority"))
                        continue;
                    var dcp = RPGDamageOverhaulAPI.buildProperty(propName, propEntry.getValue());
                    if (dcp != null)
                    {
                        var old = mergedProperties.getOrDefault(propName, null);
                        if (old != null && old.getClass().equals(dcp.getClass()))
                            dcp = dcp.mergeWith(old);
                        mergedProperties.put(propName, dcp);
                    }
                }
            }
            RPGDamageOverhaulAPI.registerDamageClass(dcName, mergedProperties.values(), ra);
        }
    }

    public static Collection<String> getDamageClassNames()
    {
        return damageClasses.keySet();
    }

    public static Collection<JsonObject> getDamageClassOriginalJsons(String damageClass)
    {
        if (!damageClasses.containsKey(damageClass))
            return Collections.emptyList();
        List<JsonObject> properties = new LinkedList<>();
        for (var config : damageClasses.get(damageClass))
        {
            properties.add(config.originalJson);
        }
        return properties;
    }

    public static void processBetterCombatOverrides(JsonObject obj)
    {
        for (Map.Entry<String, JsonElement> attacksEntry : obj.entrySet())
        {
            JsonArray arr = attacksEntry.getValue().getAsJsonArray();
            DamageClass[] dcs = new DamageClass[arr.size()];
            for (int i = 0; i < arr.size(); i++)
            {
                dcs[i] = RPGDamageOverhaulAPI.getDamageClass(arr.get(i).getAsString());
                if (dcs[i] == null)
                    Constants.LOG.warn("Damage class not found for BCOverride: {}", arr.get(i).getAsString());
            }
            RPGDamageOverhaulAPI.registerBetterCombatAttackOverrides(ResourceLocation.parse(attacksEntry.getKey()), dcs);

            Map<DamageClass, Integer> counts = new HashMap<>();
            for (DamageClass dc : dcs) {
                counts.put(dc, counts.getOrDefault(dc, 0) + 1);
            }

            Map<DamageClass, Double> fallbackItemOverrides = new HashMap<>();
            for (Map.Entry<DamageClass, Integer> count : counts.entrySet())
            {
                fallbackItemOverrides.put(count.getKey(), 1.0 / dcs.length * count.getValue());
            }
            RPGDamageOverhaulAPI.registerItemOverrides(ResourceLocation.parse(attacksEntry.getKey()), fallbackItemOverrides);
        }

    }
    public static void processDamageOverrides(JsonObject obj)
    {
        for (Map.Entry<String, JsonElement> entry : obj.entrySet())
        {
            ResourceLocation mcDamageType = ResourceLocation.parse(entry.getKey());
            Map<DamageClass, Double> overrides = new HashMap<>();
            JsonObject overridesObj = entry.getValue().getAsJsonObject();
            for (Map.Entry<String, JsonElement> override : overridesObj.entrySet())
            {
                DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(override.getKey());
                if (dc == null)
                {
                    Constants.LOG.warn("Unknown damage class: {} for override: {}", override.getKey(), mcDamageType);
                    continue;
                }
                overrides.put(dc, override.getValue().getAsDouble());
            }
            RPGDamageOverhaulAPI.registerOverride(mcDamageType,overrides);
        }
    }
    public static void processItemOverrides(JsonObject obj)
    {
        for (Map.Entry<String, JsonElement> itemOverride : obj.entrySet()) {
            boolean isTag;
            ResourceLocation id;
            if (itemOverride.getKey().startsWith("#"))
            {
                isTag = true;
                id = ResourceLocation.parse(itemOverride.getKey().substring(1));
            }
            else
            {
                isTag = false;
                id = ResourceLocation.parse(itemOverride.getKey());
            }
            Map<DamageClass, Double> overrides = new HashMap<>();
            for (Map.Entry<String, JsonElement> overrideEntry : itemOverride.getValue().getAsJsonObject().entrySet()) {
                DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(overrideEntry.getKey());
                if (dc != null)
                    overrides.put(dc, overrideEntry.getValue().getAsDouble());
            }
            if (!isTag)
                RPGDamageOverhaulAPI.registerItemOverrides(id, overrides);
            else
                RPGDamageOverhaulAPI.registerItemTagOverrides(id, overrides);
        }

    }

    public static void processEntityOverrides(JsonObject obj)
    {
        for (Map.Entry<String, JsonElement> entityOverride : obj.entrySet()) {
            boolean isTag = false;
            ResourceLocation id;
            if (entityOverride.getKey().startsWith("#"))
            {
                isTag = true;
                id = ResourceLocation.parse(entityOverride.getKey().substring(1));
            }
            else
            {
                id = ResourceLocation.parse(entityOverride.getKey());
            }
            Map<DamageClass, Double> overrides = new HashMap<>();
            for (Map.Entry<String, JsonElement> overrideEntry : entityOverride.getValue().getAsJsonObject().entrySet()) {
                DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(overrideEntry.getKey());
                if (dc != null)
                    overrides.put(dc, overrideEntry.getValue().getAsDouble());
            }
            if (isTag)
                RPGDamageOverhaulAPI.registerEntityTagOverrides(id, overrides);
            else
                RPGDamageOverhaulAPI.registerEntityOverrides(id, overrides);
        }
    }
}
