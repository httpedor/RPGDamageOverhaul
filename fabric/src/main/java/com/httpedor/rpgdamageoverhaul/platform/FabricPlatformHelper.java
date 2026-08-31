package com.httpedor.rpgdamageoverhaul.platform;

import com.httpedor.rpgdamageoverhaul.DamageClassRegisteredCallback;
import com.httpedor.rpgdamageoverhaul.platform.services.IPlatformHelper;
import com.httpedor.rpgdamageoverhaul.api.DamageClass;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {

        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {

        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public void updateEntityAttributes(EntityType<? extends LivingEntity> entityType, AttributeSupplier.Builder builder)
    {
        FabricDefaultAttributeRegistry.register(entityType, builder);
    }

    @Override
    public void fireDamageClassRegisteredEvent(DamageClass dc)
    {
        DamageClassRegisteredCallback.EVENT.invoker().interact(dc);
    }
}
