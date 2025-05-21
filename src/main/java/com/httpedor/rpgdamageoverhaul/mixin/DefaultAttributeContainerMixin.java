package com.httpedor.rpgdamageoverhaul.mixin;

import com.httpedor.rpgdamageoverhaul.ducktypes.CopyableDefaultAttrContainer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(AttributeSupplier.class)
public class DefaultAttributeContainerMixin implements CopyableDefaultAttrContainer {


    @Shadow @Final private Map<Attribute, AttributeInstance> instances;

    @Override
    public void copyTo(AttributeSupplier.Builder builder) {
        for(Attribute entityAttribute : instances.keySet()) {
            AttributeInstance entityAttributeInstance = this.instances.get(entityAttribute);
            double value = entityAttributeInstance.getBaseValue();
            builder.add(entityAttribute, value);
        }
    }
}
