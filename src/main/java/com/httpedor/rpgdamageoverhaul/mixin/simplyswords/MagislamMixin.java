package com.httpedor.rpgdamageoverhaul.mixin.simplyswords;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.player.PlayerEntity;
import net.sweenus.simplyswords.effect.MagislamEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MagislamEffect.class)
public class MagislamMixin {

    @WrapOperation(method = "applyUpdateEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/damage/DamageSources;playerAttack(Lnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/entity/damage/DamageSource;"))
    public DamageSource arcaneDamage(DamageSources instance, PlayerEntity attacker, Operation<DamageSource> original)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass("arcane");
        if (dc == null)
            return original.call(instance, attacker);
        return dc.createDamageSource(attacker);
    }

}
