package com.httpedor.rpgdamageoverhaul.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

@Mixin(targets = "net.minecraft.world.effect.PoisonMobEffect")
public abstract class PoisonEffectMixin extends MobEffect {

    public PoisonEffectMixin(MobEffectCategory mobEffectCategory, int i) {
        super(mobEffectCategory, i);
    }

    @WrapOperation(method = "applyEffectTick", at = @At(value = "NEW", target = "net/minecraft/world/damagesource/DamageSource"))
    private DamageSource poisonDamageType(Holder<DamageType> instance, Operation<DamageSource> original)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass("poison");
        // TODO: On-hit exceptions instead of just not triggering all of them
        // that way, poison can still antiheal on the potion effect
        if (dc != null)
            return dc.createDamageSource(false);
        return original.call(instance);
    }

}
