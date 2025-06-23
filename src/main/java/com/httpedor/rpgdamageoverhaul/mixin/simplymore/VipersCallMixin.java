package com.httpedor.rpgdamageoverhaul.mixin.simplymore;

import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.rosemarythyme.simplymore.entity.VipersCallProjectileAreaEffectCloudEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VipersCallProjectileAreaEffectCloudEntity.class)
public class VipersCallMixin {

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/damage/DamageSources;magic()Lnet/minecraft/entity/damage/DamageSource;"))
    public DamageSource poisonDmg(DamageSources instance, Operation<DamageSource> original)
    {
        var poisonDmg = RPGDamageOverhaulAPI.getDamageClass("poison");
        if (poisonDmg != null)
            return poisonDmg.createDamageSource();
        return original.call(instance);
    }

}
