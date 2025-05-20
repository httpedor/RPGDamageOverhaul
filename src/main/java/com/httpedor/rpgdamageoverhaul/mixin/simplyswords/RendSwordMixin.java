package com.httpedor.rpgdamageoverhaul.mixin.simplyswords;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.sweenus.simplyswords.item.custom.RendSwordItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RendSwordItem.class)
public class RendSwordMixin {

    @WrapOperation(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"))
    private boolean soulDamage(LivingEntity instance, DamageSource source, float amount, Operation<Boolean> original)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass("soul");
        if(dc != null)
            return original.call(instance, dc.createDamageSource(source.getAttacker(), source.getSource()), amount);
        return original.call(instance, source, amount);
    }

}
