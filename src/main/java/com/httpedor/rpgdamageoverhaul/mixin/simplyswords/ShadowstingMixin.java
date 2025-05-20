package com.httpedor.rpgdamageoverhaul.mixin.simplyswords;

import com.httpedor.rpgdamageoverhaul.RPGDamageOverhaul;
import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.sweenus.simplyswords.item.custom.ShadowstingSwordItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ShadowstingSwordItem.class)
public class ShadowstingMixin {

    @WrapOperation(method = "postHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/damage/DamageSources;indirectMagic(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity;)Lnet/minecraft/entity/damage/DamageSource;"))
    public DamageSource arcaneDamage(DamageSources instance, Entity source, Entity attacker, Operation<DamageSource> original)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass("arcane");
        if (dc == null)
            return original.call(instance, source, attacker);
        return dc.createDamageSource(attacker, source);
    }

}
