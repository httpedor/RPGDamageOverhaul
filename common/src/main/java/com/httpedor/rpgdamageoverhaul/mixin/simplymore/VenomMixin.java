package com.httpedor.rpgdamageoverhaul.mixin.simplymore;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.rosemarythyme.simplymore.effect.VenomPoisonEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VenomPoisonEffect.class)
public class VenomMixin {

    @WrapOperation(method = "applyEffectTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;magic()Lnet/minecraft/world/damagesource/DamageSource;", ordinal = 0))
    public DamageSource poison(DamageSources instance, Operation<DamageSource> original)
    {
        DamageClass poison = RPGDamageOverhaulAPI.getDamageClass("poison");
        if (poison != null)
            return poison.createDamageSource();
        return original.call(instance);
    }

    @WrapOperation(method = "applyEffectTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;magic()Lnet/minecraft/world/damagesource/DamageSource;", ordinal = 1))
    public DamageSource poison2(DamageSources instance, Operation<DamageSource> original)
    {
        DamageClass poison = RPGDamageOverhaulAPI.getDamageClass("poison");
        if (poison != null)
            return poison.createDamageSource();
        return original.call(instance);
    }
}
