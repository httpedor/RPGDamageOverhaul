package com.httpedor.rpgdamageoverhaul.mixin.simplyswords;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.sweenus.simplyswords.item.custom.StealSwordItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(StealSwordItem.class)
public class StealSwordMixin {

    @WrapOperation(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean soulDamage(LivingEntity instance, DamageSource source, float amount, Operation<Boolean> original)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass("soul");
        if(dc != null)
            return original.call(instance, dc.createDamageSource(source.getEntity(), source.getDirectEntity()), amount);
        return original.call(instance, source, amount);
    }

}
