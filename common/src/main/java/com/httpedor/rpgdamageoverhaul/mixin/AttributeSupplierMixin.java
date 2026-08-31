package com.httpedor.rpgdamageoverhaul.mixin;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.httpedor.rpgdamageoverhaul.ducktypes.CopyableDefaultAttrContainer;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

@Mixin(AttributeSupplier.class)
public class AttributeSupplierMixin implements CopyableDefaultAttrContainer {
    
    @Shadow @Final private Map<Holder<Attribute>, AttributeInstance> instances;

    @Override
    public void copyTo(AttributeSupplier.Builder builder) {
        for(var attribute : instances.keySet()) {
            AttributeInstance attributeInstance = this.instances.get(attribute);
            double value = attributeInstance.getBaseValue();
            builder.add(attribute, value);
        }
    }

    @Override
    public Collection<Holder<Attribute>> getAttributes() {
        return instances.keySet();
    }
}
