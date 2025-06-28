package com.httpedor.rpgdamageoverhaul.mixin.simplymore;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.rosemarythyme.simplymore.entity.PoisonBoltAreaEffectCloudEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PoisonBoltAreaEffectCloudEntity.class)
public class PoisonBoltMixin {

    @WrapOperation(method = "judder", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;magic()Lnet/minecraft/world/damagesource/DamageSource;"))
    public DamageSource poisonDmg(DamageSources instance, Operation<DamageSource> original)
    {
        DamageClass poison = RPGDamageOverhaulAPI.getDamageClass("poison");
        if (poison != null)
            return poison.createDamageSource();
        return original.call(instance);
    }

}
