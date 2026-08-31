package com.httpedor.rpgdamageoverhaul;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class JsonHelpers {
    public static AttributeModifier parseAttributeModifier(ResourceLocation id, JsonElement json)
    {
        AttributeModifier.Operation op = AttributeModifier.Operation.ADD_VALUE;
        double value;
        if (json.isJsonObject())
        {
            JsonObject valObj = json.getAsJsonObject();
            String opStr = valObj.get("operation").getAsString();
            op = switch (opStr.toLowerCase()) {
                case "add", "add_value" -> AttributeModifier.Operation.ADD_VALUE;
                case "multiply_base", "add_multiplied_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                case "multiply_total", "add_multiplied_total" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
                default -> {
                    Constants.LOG.warn("Unknown operation: {} for damage class potions", opStr);
                    yield AttributeModifier.Operation.ADD_VALUE;
                }
            };
            if (valObj.has("value"))
            {
                var valEl = valObj.get("value");
                if (valEl.isJsonPrimitive() && valEl.getAsJsonPrimitive().isNumber())
                    value = valObj.get("value").getAsDouble();
                else
                {
                    Constants.LOG.warn("Invalid value for potion effect: {} for damage class potions", valEl);
                    return null;
                }
            }
            else
            {
                Constants.LOG.warn("Missing value for potion effect: {} for damage class potions", json);
                return null;
            }
        }
        else if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber())
            value = json.getAsDouble();
        else
        {
            Constants.LOG.warn("Invalid value for potion effect: {} for damage class potions", json);
            return null;
        }

        return new AttributeModifier(id, value, op);
    }
}
