package com.httpedor.rpgdamageoverhaul.mixin.simplyswords;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.player.PlayerEntity;
import net.sweenus.simplyswords.item.custom.EmberIreSwordItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EmberIreSwordItem.class)
public class EmberIreMixin {

    @WrapOperation(method = "onStoppedUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/damage/DamageSources;generic()Lnet/minecraft/entity/damage/DamageSource;"))
    public DamageSource mobFireDamage(DamageSources instance, Operation<DamageSource> original)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass("fire");
        if (dc != null)
            return dc.createDamageSource();
        return original.call(instance);
    }

    @WrapOperation(method = "onStoppedUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/damage/DamageSources;playerAttack(Lnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/entity/damage/DamageSource;"))
    public DamageSource playerFireDamage(DamageSources instance, PlayerEntity attacker, Operation<DamageSource> original)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass("fire");
        if (dc != null)
            return dc.createDamageSource(attacker);
        return original.call(instance, attacker);
    }
}
