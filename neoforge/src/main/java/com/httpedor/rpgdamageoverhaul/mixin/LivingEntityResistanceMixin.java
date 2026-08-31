package com.httpedor.rpgdamageoverhaul.mixin;

import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.httpedor.rpgdamageoverhaul.SharedLogic;

import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.damagesource.DamageSource;

@Mixin(LivingEntity.class)
public class LivingEntityResistanceMixin {
    @ModifyVariable(method = "actuallyHurt", at = @At("STORE"), ordinal = 3)
    private float modifyDamage(float damage, @Local(argsOnly = true) DamageSource source)
    {
        return SharedLogic.getDamagePostResistance(damage, (LivingEntity)(Object)this, source.type());
    }
}
