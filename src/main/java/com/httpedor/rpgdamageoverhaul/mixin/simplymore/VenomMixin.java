package com.httpedor.rpgdamageoverhaul.mixin.simplymore;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.rosemarythyme.simplymore.effect.VenomPoisonEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VenomPoisonEffect.class)
public class VenomMixin {

    @WrapOperation(method = "applyUpdateEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/damage/DamageSources;magic()Lnet/minecraft/entity/damage/DamageSource;", ordinal = 0))
    public DamageSource poison(DamageSources instance, Operation<DamageSource> original)
    {
        DamageClass poison = RPGDamageOverhaulAPI.getDamageClass("poison");
        if (poison != null)
            return poison.createDamageSource();
        return original.call(instance);
    }

    @WrapOperation(method = "applyUpdateEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/damage/DamageSources;magic()Lnet/minecraft/entity/damage/DamageSource;", ordinal = 1))
    public DamageSource poison2(DamageSources instance, Operation<DamageSource> original)
    {
        DamageClass poison = RPGDamageOverhaulAPI.getDamageClass("poison");
        if (poison != null)
            return poison.createDamageSource();
        return original.call(instance);
    }
}
