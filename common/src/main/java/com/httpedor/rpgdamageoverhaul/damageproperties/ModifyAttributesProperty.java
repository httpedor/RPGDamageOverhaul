package com.httpedor.rpgdamageoverhaul.damageproperties;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.httpedor.rpgdamageoverhaul.Constants;
import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.DamageClass.DCAttribute;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.httpedor.rpgdamageoverhaul.api.DamageClassProperty;

import net.minecraft.resources.ResourceLocation;

public abstract class ModifyAttributesProperty<TK, TV> extends DamageClassProperty {
    protected final Map<TK, Map<DCAttribute, TV>> attributeModifiers;

    public ModifyAttributesProperty(Map<TK, Map<DCAttribute, TV>> attributeModifiers) {
        this.attributeModifiers = attributeModifiers;
    }

    public Map<TK, TV> getModifiersForAttribute(DCAttribute attribute) {
        Map<TK, TV> result = new HashMap<>();
        for (var entry : attributeModifiers.entrySet()) {
            TK key = entry.getKey();
            Map<DCAttribute, TV> modifiers = entry.getValue();
            if (modifiers.containsKey(attribute)) {
                result.put(key, modifiers.get(attribute));
            }
        }
        return result;
    }

    public static <TK, TV> Map<TK, Map<DCAttribute, TV>> fromJson(JsonElement json, Function<String, TK> keyParser, BiFunction<JsonElement, ResourceLocation, TV> valueParser, String propertyName)
    {
        Map<TK, Map<DamageClass.DCAttribute, TV>> attributeModifiers = new HashMap<>();
        JsonObject jsonObj = json.getAsJsonObject();
        for (Map.Entry<String, JsonElement> attribute : jsonObj.entrySet())
        {
            DCAttribute dcAttr = switch (attribute.getKey()) {
                case "resistance" -> DamageClass.DCAttribute.RESISTANCE;
                case "damage" -> DamageClass.DCAttribute.DAMAGE;
                case "armor" -> DamageClass.DCAttribute.ARMOR;
                case "absorption" -> DamageClass.DCAttribute.ABSORPTION;
                default -> null;
            };
            if (dcAttr == null)                {
                Constants.LOG.warn("Unknown attribute: {} for damage class " + propertyName + "s", attribute.getKey());
                continue;
            }
            JsonObject attrs = attribute.getValue().getAsJsonObject();
            int i = 0;
            for (Map.Entry<String, JsonElement> attr : attrs.entrySet())
            {
                var key = keyParser.apply(attr.getKey());
                if (key == null)
                {
                    Constants.LOG.warn("Unknown " + propertyName + ": {} for damage class " + propertyName + "s", attr.getKey());
                    continue;
                }
                var valEl = attr.getValue();
                ResourceLocation attrModId = ResourceLocation.fromNamespaceAndPath("rpgdamageoverhaul", propertyName + "/" + (attr.getKey().toString().replace(':', '/')) + "/" + attribute.getKey() + "/" + i);
                var mod = valueParser.apply(valEl, attrModId);
                if (mod != null)
                {
                    attributeModifiers.computeIfAbsent(key, k -> new HashMap<>()).put(dcAttr, mod);
                }
                i++;
            }
        }
        return attributeModifiers;
    }

    protected Map<TK, Map<DCAttribute, TV>> mergeWith(Map<TK, Map<DCAttribute, TV>> otherModifiers) {
        var mergedModifiers = new HashMap<>(this.attributeModifiers);

        for (var entry : otherModifiers.entrySet()) {
            TK key = entry.getKey();
            var modifiers = entry.getValue();
            if (mergedModifiers.containsKey(key)) {
                var existingModifiers = mergedModifiers.get(key);
                for (var modEntry : modifiers.entrySet()) {
                    DCAttribute attribute = modEntry.getKey();
                    var modifier = modEntry.getValue();
                    if (!existingModifiers.containsKey(attribute))
                    {
                        existingModifiers.put(attribute, modifier);
                    }
                }
            }
            else {
                mergedModifiers.put(key, modifiers);
            }
        }

        return mergedModifiers;
    }
}
