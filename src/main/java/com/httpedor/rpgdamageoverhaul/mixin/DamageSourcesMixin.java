package com.httpedor.rpgdamageoverhaul.mixin;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(DamageSources.class)
public class DamageSourcesMixin {

    @Inject(method = "source(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/damagesource/DamageSource;", at = @At("HEAD"), cancellable = true)
    public void entityOverride(ResourceKey<DamageType> dt, Entity direct, Entity causing, CallbackInfoReturnable<DamageSource> cir)
    {
        /*Map<DamageClass, Double> overrides = RPGDamageOverhaulAPI.getEntityOverrides(direct);
        if (overrides.isEmpty())
            return;

        double biggest = 0;
        DamageClass dc = null;
        for (var entry : overrides.entrySet())
        {
            if (entry.getValue() > biggest)
            {
                biggest = entry.getValue();
                dc = entry.getKey();
            }
        }

        cir.setReturnValue(dc.createDamageSource(direct, causing));*/
    }
}
