package com.httpedor.rpgdamageoverhaul.mixin.simplyswords;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.sweenus.simplyswords.effect.FireVortexEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FireVortexEffect.class)
public class FireVortexMixin {

    @WrapOperation(method = "applyEffectTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean fireDamage(LivingEntity instance, DamageSource source, float amount, Operation<Boolean> original)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass("fire");
        if (dc == null)
            return original.call(instance, source, amount);
        return original.call(instance, dc.createDamageSource(source.getEntity(), source.getDirectEntity()), amount);
    }

}
