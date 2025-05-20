package com.httpedor.rpgdamageoverhaul.mixin.simplyswords;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.sweenus.simplyswords.item.custom.LichbladeSwordItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LichbladeSwordItem.class)
public class LichbladeMixin {

    @WrapOperation(method = "inventoryTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/damage/DamageSources;indirectMagic(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity;)Lnet/minecraft/entity/damage/DamageSource;"))
    private DamageSource correctDamageType(DamageSources instance, Entity source, Entity attacker, Operation<DamageSource> original) {
        DamageClass soulDmg = RPGDamageOverhaulAPI.getDamageClass("soul");
        if (soulDmg == null)
            return original.call(instance, source, attacker);
        return soulDmg.createDamageSource(attacker, source);
    }

}
