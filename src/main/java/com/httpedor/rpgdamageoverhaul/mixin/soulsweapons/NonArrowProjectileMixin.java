package com.httpedor.rpgdamageoverhaul.mixin.soulsweapons;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import net.minecraft.entity.damage.DamageSource;
import net.soulsweaponry.entity.projectile.NonArrowProjectile;
import net.soulsweaponry.entity.projectile.SilverBulletEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(NonArrowProjectile.class)
public class NonArrowProjectileMixin {

    @ModifyArg(method = "onEntityHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"), index = 0)
    private DamageSource moonlightArcaneDmg(DamageSource source) {
        if (source.getSource() instanceof SilverBulletEntity)
            return source;

        DamageClass arcane = RPGDamageOverhaulAPI.getDamageClass("arcane");
        if (arcane == null)
            return source;

        return arcane.createDamageSource(source.getSource(), source.getAttacker());
    }

}
