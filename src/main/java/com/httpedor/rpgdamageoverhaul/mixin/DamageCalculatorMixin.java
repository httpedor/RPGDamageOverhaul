package com.httpedor.rpgdamageoverhaul.mixin;

import net.minecraft.entity.DamageUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DamageUtil.class)
public class DamageCalculatorMixin {

    //Formula from https://github.com/Jackiecrazy/ArmorCurve/blob/master/src/main/java/jackiecrazy/armorcurve/CurveConfig.java
    @Inject(method = "getDamageLeft", at = @At("HEAD"), cancellable = true)
    private static void reworkArmor(float damage, float armor, float armorToughness, CallbackInfoReturnable<Float> cir)
    {
        cir.setReturnValue(damage * Math.max(15/(15+armor+Math.min(armorToughness, damage)), 0.1f));
    }

}
