package com.httpedor.rpgdamageoverhaul.mixin;

import com.httpedor.rpgdamageoverhaul.RPGDamageOverhaul;
import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.DamageHandler;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.httpedor.rpgdamageoverhaul.compat.BetterCombatCompat;
import com.httpedor.rpgdamageoverhaul.ducktypes.DCDamageSource;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(PlayerEntity.class)
public abstract class PlayerMixin extends LivingEntity {


    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Shadow
    protected abstract void applyDamage(DamageSource source, float amount);

    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/enchantment/EnchantmentHelper;getFireAspect(Lnet/minecraft/entity/LivingEntity;)I"))
    private void enchantmentDamage(Entity target, CallbackInfo ci)
    {
        for (var entry : RPGDamageOverhaul.damageEnchantments.entrySet())
        {
            var enchantment = entry.getKey();
            var dc = entry.getValue().getLeft();
            var multiplier = entry.getValue().getRight();
            int level = EnchantmentHelper.getEquipmentLevel(enchantment, this);
            if (level > 0)
            {
                target.damage(dc.createDamageSource(this), level * multiplier);
            }
        }
    }

    @WrapOperation(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"))
    private boolean otherDamageAttacks(Entity target, DamageSource source, float amount, Operation<Boolean> original)
    {
        boolean ret = false;
        for (DamageClass dc : RPGDamageOverhaulAPI.getAllDamageClasses())
        {
            double dmg = getAttributeValue(dc.dmgAttribute);
            if (dmg > 0)
            {
                ret |= target.damage(dc.createDamageSource(this), (float)dmg);
            }
        }
        if (FabricLoader.getInstance().isModLoaded("bettercombat"))
        {
            if (BetterCombatCompat.shouldBCHandleAttack((PlayerEntity)(Object)this))
                return original.call(target, source, amount) || ret;
        }

        var is = getMainHandStack();
        Map<DamageClass, Double> newDamages = new HashMap<>();
        RPGDamageOverhaulAPI.applyItemOverrides(is, newDamages);
        if (newDamages.isEmpty())
        {
            DamageClass blunt = RPGDamageOverhaulAPI.getDamageClass("blunt");
            if (blunt != null)
                return ret || target.damage(blunt.createDamageSource(this), amount);
            else
                return ret || original.call(target, source, amount);
        }
        else
        {
            for (var entry : newDamages.entrySet())
            {
                var dc = entry.getKey();
                var dmg = entry.getValue();
                ret |= target.damage(dc.createDamageSource(this), dmg.floatValue());
            }
            return ret;
        }
    }


    @Inject(method = "applyDamage", at = @At("HEAD"), cancellable = true)
    private void damageOverrides(DamageSource source, float amount, CallbackInfo ci)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.getType());
        if (dc != null)
        {
            if (((DCDamageSource)source).shouldTriggerOnHitEffects())
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
