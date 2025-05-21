package com.httpedor.rpgdamageoverhaul.mixin.simplyswords;

import com.httpedor.rpgdamageoverhaul.RPGDamageOverhaul;
import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.sweenus.simplyswords.item.custom.ShadowstingSwordItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ShadowstingSwordItem.class)
public class ShadowstingMixin {

    @WrapOperation(method = "hurtEnemy", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;indirectMagic(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/damagesource/DamageSource;"))
    public DamageSource arcaneDamage(DamageSources instance, Entity source, Entity attacker, Operation<DamageSource> original)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass("arcane");
        if (dc == null)
            return original.call(instance, source, attacker);
        return dc.createDamageSource(attacker, source);
    }

}
