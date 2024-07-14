package com.httpedor.rpgdamageoverhaul.mixin;

import com.httpedor.rpgdamageoverhaul.ducktypes.CopyableDefaultAttrContainer;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(DefaultAttributeContainer.class)
public class DefaultAttributeContainerMixin implements CopyableDefaultAttrContainer {


    @Shadow @Final private Map<EntityAttribute, EntityAttributeInstance> instances;

    @Override
    public void copyTo(DefaultAttributeContainer.Builder builder) {
        for(EntityAttribute entityAttribute : instances.keySet()) {
            EntityAttributeInstance entityAttributeInstance = this.instances.get(entityAttribute);
            double value = entityAttributeInstance.getBaseValue();
            builder.add(entityAttribute, value);
        }
    }
}
