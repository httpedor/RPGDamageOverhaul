package com.httpedor.rpgdamageoverhaul.platform;

import java.util.Map;

import com.httpedor.rpgdamageoverhaul.Constants;
import com.httpedor.rpgdamageoverhaul.DamageClassRegisteredEvent;
import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.platform.services.IPlatformHelper;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;

public class NeoForgePlatformHelper implements IPlatformHelper {

    private static volatile Map<EntityType<? extends LivingEntity>, AttributeSupplier> FORGE_ATTRIBUTES_MAP;
    private static volatile boolean FORGE_ATTRIBUTES_LOOKUP_FAILED;

    @SuppressWarnings("unchecked")
    private static Map<EntityType<? extends LivingEntity>, AttributeSupplier> getForgeAttributesMap() {
        if (FORGE_ATTRIBUTES_LOOKUP_FAILED) {
            return null;
        }
        var cached = FORGE_ATTRIBUTES_MAP;
        if (cached != null) {
            return cached;
        }
        try {
            var field = CommonHooks.class.getDeclaredField("FORGE_ATTRIBUTES");
            field.setAccessible(true);
            var value = field.get(null);
            if (value instanceof Map<?, ?> map) {
                FORGE_ATTRIBUTES_MAP = (Map<EntityType<? extends LivingEntity>, AttributeSupplier>) map;
                return FORGE_ATTRIBUTES_MAP;
            }
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException t) {
            Constants.LOG.error("Failed to access CommonHooks.FORGE_ATTRIBUTES via reflection; entity attributes may not include RPGDamageOverhaul attributes until next restart.", t);
        }
        FORGE_ATTRIBUTES_LOOKUP_FAILED = true;
        return null;
    }

    @Override
    public String getPlatformName() {

        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {

        return ModList.get().isLoaded(modId);
    }

    @Override
    public void updateEntityAttributes(EntityType<? extends LivingEntity> entityType, AttributeSupplier.Builder builder)
    {
        var forgeAttrs = getForgeAttributesMap();
        if (forgeAttrs != null) {
            forgeAttrs.put(entityType, builder.build());
        }
    }

    @Override
    public void fireDamageClassRegisteredEvent(DamageClass dc)
    {
        var event = new DamageClassRegisteredEvent(dc);
        NeoForge.EVENT_BUS.post(event);
    }

    @Override
    public boolean isDevelopmentEnvironment() {

        return !FMLLoader.isProduction();
    }
}