package com.httpedor.rpgdamageoverhaul.damageproperties;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.httpedor.rpgdamageoverhaul.api.DamageClass.DCAttribute;
import com.httpedor.rpgdamageoverhaul.api.DamageClass;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

public class EnchantmentAttributesProperty extends ModifyAttributesProperty<ResourceKey<Enchantment>, Function<Integer, Float>> {
    private final Map<ResourceKey<Enchantment>, Map<DCAttribute, Function<Integer, Float>>> attributeModifiers;

    public EnchantmentAttributesProperty(Map<ResourceKey<Enchantment>, Map<DCAttribute, Function<Integer, Float>>> attributeModifiers) {
        super(attributeModifiers);
        this.attributeModifiers = attributeModifiers;
    }

    public Map<ResourceKey<Enchantment>, Map<DCAttribute, Function<Integer, Float>>> getAttributeModifiers() {
        return attributeModifiers;
    }
}
