package com.httpedor.rpgdamageoverhaul.damageproperties;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.Map;

import com.httpedor.rpgdamageoverhaul.api.DamageClass.DCAttribute;

public class PotionAttributesProperty extends ModifyAttributesProperty<MobEffect, AttributeModifier> {

    public PotionAttributesProperty(Map<MobEffect, Map<DCAttribute, AttributeModifier>> attributeModifiers) {
        super(attributeModifiers);
    }

    @Override
    public void onRegistered(DamageClass dc) {
        for (var entry : attributeModifiers.entrySet()) {
            MobEffect effect = entry.getKey();
            Map<DCAttribute, AttributeModifier> modifiers = entry.getValue();
            for (var modEntry : modifiers.entrySet()) {
                DCAttribute dcAttr = modEntry.getKey();
                AttributeModifier modifier = modEntry.getValue();
                var attr = dc.getAttribute(dcAttr);
                effect.addAttributeModifier(attr, modifier.id(), modifier.amount(), modifier.operation());
            }
        }
    }
}
