package com.httpedor.rpgdamageoverhaul;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.*;

public class AttributeUtils {
    public static float applyAttributeModifiers(float base, Map<AttributeModifier.Operation, Collection<AttributeModifier>> modifiers)
    {
        float newBase = base;
        for (var entry : modifiers.getOrDefault(AttributeModifier.Operation.ADD_VALUE, List.of()))
            newBase += (float) entry.amount();
        float result = newBase;
        for (var entry : modifiers.getOrDefault(AttributeModifier.Operation.ADD_MULTIPLIED_BASE, List.of()))
            result += (float) (entry.amount() * newBase);
        for (var entry : modifiers.getOrDefault(AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, List.of()))
            result *= (1 + (float) entry.amount());

        return result;
    }
    public static Map<AttributeModifier.Operation, Collection<AttributeModifier>> sortModifiers(Collection<AttributeModifier> modifiers)
    {
        Map<AttributeModifier.Operation, Collection<AttributeModifier>> sorted = Map.of(
                AttributeModifier.Operation.ADD_VALUE, new LinkedList<>(),
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE, new LinkedList<>(),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, new LinkedList<>()
        );
        for (var modifier : modifiers)
        {
            sorted.get(modifier.operation()).add(modifier);
        }
        return sorted;
    }

    public static float applyAttributeModifiers(float base, Collection<AttributeModifier> modifiers)
    {
        return applyAttributeModifiers(base, sortModifiers(modifiers));
    }
}
