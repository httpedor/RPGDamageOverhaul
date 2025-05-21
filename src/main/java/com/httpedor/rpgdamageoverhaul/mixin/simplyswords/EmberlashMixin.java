package com.httpedor.rpgdamageoverhaul.mixin.simplyswords;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.player.Player;
import net.sweenus.simplyswords.item.custom.EmberlashSwordItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EmberlashSwordItem.class)
public class EmberlashMixin {

    @WrapOperation(method = "hurtEnemy", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;generic()Lnet/minecraft/world/damagesource/DamageSource;"))
    public DamageSource mobFireDamage(DamageSources instance, Operation<DamageSource> original)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass("fire");
        if (dc != null)
            return dc.createDamageSource();
        return original.call(instance);
    }

    @WrapOperation(method = "hurtEnemy", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;playerAttack(Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/damagesource/DamageSource;"))
    public DamageSource playerFireDamage(DamageSources instance, Player attacker, Operation<DamageSource> original)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass("fire");
        if (dc != null)
            return dc.createDamageSource(attacker);
        return original.call(instance, attacker);
    }
}
