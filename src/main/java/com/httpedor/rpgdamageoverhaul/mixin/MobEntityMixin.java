package com.httpedor.rpgdamageoverhaul.mixin;

import com.httpedor.rpgdamageoverhaul.DamageClass;
import com.httpedor.rpgdamageoverhaul.RPGDamageOverhaulAPI;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin extends LivingEntity {

    protected MobEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "tryAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setOnFireFor(I)V"))
    private void fireDamage(Entity target, CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 0) int i)
    {
        var fireDc = RPGDamageOverhaulAPI.getDamageClass("fire");
        if (fireDc != null)
            target.damage(new DamageSource(fireDc.getDamageTypeEntry(), this), 2 * i);
    }

    @Inject(method = "tryAttack", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"))
    private void damageAttributes(Entity target, CallbackInfoReturnable<Boolean> cir)
    {
        for (DamageClass dc : RPGDamageOverhaulAPI.getAllDamageClasses())
        {
            double dmg = getAttributeValue(dc.dmgAttribute);
            if (dmg > 0)
                target.damage(new DamageSource(dc.getDamageTypeEntry(), this), (float)dmg);
        }
    }

}
