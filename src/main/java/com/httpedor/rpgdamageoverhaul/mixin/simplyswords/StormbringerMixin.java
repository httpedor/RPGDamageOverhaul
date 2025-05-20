package com.httpedor.rpgdamageoverhaul.mixin.simplyswords;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.sweenus.simplyswords.item.custom.StormbringerSwordItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(StormbringerSwordItem.class)
public class StormbringerMixin {

    @WrapOperation(method = "onStoppedUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/damage/DamageSources;indirectMagic(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity;)Lnet/minecraft/entity/damage/DamageSource;"))
    public DamageSource lightningDamage(DamageSources instance, Entity source, Entity attacker, Operation<DamageSource> original)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass("lightning");
        if(dc != null)
            return dc.createDamageSource(attacker, source);
        return original.call(instance, source, attacker);
    }

}
