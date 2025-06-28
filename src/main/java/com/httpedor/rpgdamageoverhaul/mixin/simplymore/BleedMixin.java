package com.httpedor.rpgdamageoverhaul.mixin.simplymore;

import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.rosemarythyme.simplymore.effect.BleedPoisonEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BleedPoisonEffect.class)
public class BleedMixin {

    @WrapOperation(method = "applyEffectTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;magic()Lnet/minecraft/world/damagesource/DamageSource;"))
    public DamageSource trueDmg(DamageSources instance, Operation<DamageSource> original)
    {
        var trueDC = RPGDamageOverhaulAPI.getDamageClass("true");
        if (trueDC != null)
            return trueDC.createDamageSource();
        return original.call(instance);
    }

}
