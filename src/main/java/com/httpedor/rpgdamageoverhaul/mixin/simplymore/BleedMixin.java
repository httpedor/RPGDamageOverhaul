package com.httpedor.rpgdamageoverhaul.mixin.simplymore;

import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.rosemarythyme.simplymore.effect.BleedPoisonEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BleedPoisonEffect.class)
public class BleedMixin {

    @WrapOperation(method = "applyUpdateEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/damage/DamageSources;magic()Lnet/minecraft/entity/damage/DamageSource;"))
    public DamageSource trueDmg(DamageSources instance, Operation<DamageSource> original)
    {
        var trueDC = RPGDamageOverhaulAPI.getDamageClass("true");
        if (trueDC != null)
            return trueDC.createDamageSource();
        return original.call(instance);
    }

}
