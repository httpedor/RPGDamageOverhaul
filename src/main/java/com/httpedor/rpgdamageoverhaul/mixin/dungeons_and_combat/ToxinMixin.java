package com.httpedor.rpgdamageoverhaul.mixin.dungeons_and_combat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.mcreator.dungeonsandcombat.procedures.ToxinOnEffectActiveTickProcedure;

@Mixin(ToxinOnEffectActiveTickProcedure.class)
public class ToxinMixin {
    
    @WrapOperation(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private static boolean applyToxinDamage(net.minecraft.world.entity.LivingEntity instance, net.minecraft.world.damagesource.DamageSource source, float amount, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original) {
        var dc = com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI.getDamageClass("poison");
        if (dc == null)
            return original.call(instance, source, amount);
        return original.call(instance, dc.createDamageSource(source.getEntity(), source.getDirectEntity(), false), amount);
    }
}
