package com.httpedor.rpgdamageoverhaul.mixin;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.effect.StatusEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(StatusEffect.class)
public class PoisonEffectMixin {

    @WrapOperation(method = "applyUpdateEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/damage/DamageSources;magic()Lnet/minecraft/entity/damage/DamageSource;"))
    private DamageSource poisonDamage(DamageSources instance, Operation<DamageSource> original)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass("poison");
        if (dc != null)
            return dc.createDamageSource(false);
        return original.call(instance);
    }

}
