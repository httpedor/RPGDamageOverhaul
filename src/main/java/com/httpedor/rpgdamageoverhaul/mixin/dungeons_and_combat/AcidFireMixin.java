package com.httpedor.rpgdamageoverhaul.mixin.dungeons_and_combat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.mcreator.dungeonsandcombat.procedures.AcidFiredEntityProcedure;

@Mixin(AcidFiredEntityProcedure.class)
public class AcidFireMixin {
    
    @WrapOperation(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private static boolean applyAcidFireDamage(net.minecraft.world.entity.LivingEntity instance, net.minecraft.world.damagesource.DamageSource source, float amount, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original) {
        var dc = RPGDamageOverhaulAPI.getDamageClass("poison");
        var fireDc = RPGDamageOverhaulAPI.getDamageClass("fire");
        if (dc == null || fireDc == null)
            return original.call(instance, source, amount);
        return original.call(instance, fireDc.createDamageSource(source.getEntity(), source.getDirectEntity()), amount/2)
                && original.call(instance, dc.createDamageSource(source.getEntity(), source.getDirectEntity(), false), amount/2);
    }

}
