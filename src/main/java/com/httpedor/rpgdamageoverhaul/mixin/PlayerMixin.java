package com.httpedor.rpgdamageoverhaul.mixin;

import com.httpedor.rpgdamageoverhaul.DamageClass;
import com.httpedor.rpgdamageoverhaul.DamageHandler;
import com.httpedor.rpgdamageoverhaul.RPGDamageOverhaulAPI;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(PlayerEntity.class)
public abstract class PlayerMixin extends LivingEntity {


    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Shadow
    protected abstract void applyDamage(DamageSource source, float amount);


    @Inject(method = "attack", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"))
    private void damageAttributes(Entity target, CallbackInfo ci)
    {
        for (DamageClass dc : RPGDamageOverhaulAPI.getAllDamageClasses())
        {
            double dmg = getAttributeValue(dc.dmgAttribute);
            if (dmg > 0)
                target.damage(new DamageSource(getWorld().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(dc.damageType), this), (float)dmg);
        }
    }

    @Inject(method = "applyDamage", at = @At("HEAD"), cancellable = true)
    private void damageOverrides(DamageSource source, float amount, CallbackInfo ci)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.getType());
        if (dc != null)
        {
            DamageHandler.executeOnHitEffects(dc, this, source, amount);

            return;
        }

        var newDamages = DamageHandler.applyDamageOverrides(this, source, amount);
        if (newDamages != null)
        {
            for (Map.Entry<DamageSource, Double> entry : newDamages.entrySet())
                applyDamage(entry.getKey(), entry.getValue().floatValue());
            ci.cancel();
        }
    }
}
