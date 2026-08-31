package com.httpedor.rpgdamageoverhaul.mixin.simplymore;

import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.rosemarythyme.simplymore.entity.VipersCallProjectileAreaEffectCloudEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VipersCallProjectileAreaEffectCloudEntity.class)
public class VipersCallMixin {

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;magic()Lnet/minecraft/world/damagesource/DamageSource;"))
    public DamageSource poisonDmg(DamageSources instance, Operation<DamageSource> original)
    {
        var poisonDmg = RPGDamageOverhaulAPI.getDamageClass("poison");
        if (poisonDmg != null)
            return poisonDmg.createDamageSource();
        return original.call(instance);
    }

}
