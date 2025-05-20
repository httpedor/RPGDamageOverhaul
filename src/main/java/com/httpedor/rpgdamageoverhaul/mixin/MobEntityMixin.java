package com.httpedor.rpgdamageoverhaul.mixin;

import com.httpedor.rpgdamageoverhaul.RPGDamageOverhaul;
import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.httpedor.rpgdamageoverhaul.compat.BetterCombatCompat;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin extends LivingEntity {

    @Shadow public abstract Iterable<ItemStack> getHandItems();

    protected MobEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "tryAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/enchantment/EnchantmentHelper;getFireAspect(Lnet/minecraft/entity/LivingEntity;)I"))
    private void fireDamage(Entity target, CallbackInfoReturnable<Boolean> cir)
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

    @WrapOperation(method = "tryAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"))
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

        double physicalDamage = getAttributeBaseValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        double totalPhysicalDamage = getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        for (DamageClass dc : RPGDamageOverhaulAPI.getAllDamageClasses())
        {
            if (dc.dmgAttribute == EntityAttributes.GENERIC_ATTACK_DAMAGE)
                continue;
            double dmg = getAttributeBaseValue(dc.dmgAttribute);
            double totalDmg = getAttributeValue(dc.dmgAttribute);
            if (dc.isChildOf("physical"))
            {
                physicalDamage += dmg;
                totalPhysicalDamage += totalDmg;
            }
        }

        if (FabricLoader.getInstance().isModLoaded("bettercombat"))
        {
            if (BetterCombatCompat.shouldBCHandleAttack((MobEntity)(Object)this))
                return original.call(target, source, amount) || ret;
        }
        Map<DamageClass, Double> newDamages = new HashMap<>();
        for (ItemStack is : getHandItems())
        {
            if (is.isEmpty() || is.getItem() == Items.AIR)
                continue;
            RPGDamageOverhaulAPI.applyItemOverrides(is, newDamages, physicalDamage);
        }

        if (newDamages.isEmpty())
            RPGDamageOverhaulAPI.applyEntityOverrides(this, newDamages, (float)totalPhysicalDamage);

        if (newDamages.isEmpty())
            return target.damage(source, (float)totalPhysicalDamage) || ret;
        for (var entry : newDamages.entrySet())
        {
            var dc = entry.getKey();
            var dmg = entry.getValue();
            ret |= target.damage(dc.createDamageSource(this), dmg.floatValue());
        }

        return ret;
    }


}
