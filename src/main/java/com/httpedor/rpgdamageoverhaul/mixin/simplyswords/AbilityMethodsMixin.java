package com.httpedor.rpgdamageoverhaul.mixin.simplyswords;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.sweenus.simplyswords.util.AbilityMethods;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbilityMethods.class)
public class AbilityMethodsMixin {

    @WrapOperation(method = "tickAbilitySoulAnguish", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;indirectMagic(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/damagesource/DamageSource;"))
    private static DamageSource soulAnguish(DamageSources instance, Entity source, Entity attacker, Operation<DamageSource> original)
    {
        DamageClass soulDmg = RPGDamageOverhaulAPI.getDamageClass("soul");
        if (soulDmg == null)
            return original.call(instance, source, attacker);
        return soulDmg.createDamageSource(attacker, source);
    }

    @WrapOperation(method = "tickAbilityPermafrost", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private static boolean permafrost(LivingEntity instance, DamageSource source, float amount, Operation<Boolean> original)
    {
        DamageClass frostDmg = RPGDamageOverhaulAPI.getDamageClass("frost");
        if (frostDmg == null)
            return original.call(instance, source, amount);
        return original.call(instance, frostDmg.createDamageSource(source.getEntity(), source.getDirectEntity()), amount);
    }

    @WrapOperation(method = "tickAbilityStorm", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private static boolean storm(LivingEntity instance, DamageSource source, float amount, Operation<Boolean> original)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass("lightning");
        if (dc == null)
            return original.call(instance, source, amount);

        return original.call(instance, dc.createDamageSource(source.getEntity(), source.getDirectEntity()), amount);
    }

    @WrapOperation(method = "tickAbilityThunderBlitz", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", ordinal = 0))
    private static boolean thunderBlitz1(LivingEntity instance, DamageSource source, float amount, Operation<Boolean> original)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass("lightning");
        if (dc == null)
            return original.call(instance, source, amount);

        return original.call(instance, dc.createDamageSource(source.getEntity(), source.getDirectEntity()), amount);
    }
    @WrapOperation(method = "tickAbilityThunderBlitz", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", ordinal = 1))
    private static boolean thunderBlitz2(LivingEntity instance, DamageSource source, float amount, Operation<Boolean> original)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass("lightning");
        if (dc == null)
            return original.call(instance, source, amount);

        return original.call(instance, dc.createDamageSource(source.getEntity(), source.getDirectEntity()), amount);
    }
}
