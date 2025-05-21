package com.httpedor.rpgdamageoverhaul.mixin;

import com.httpedor.rpgdamageoverhaul.RPGDamageOverhaul;
import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.httpedor.rpgdamageoverhaul.compat.BetterCombatCompat;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

@Mixin(Mob.class)
public abstract class MobEntityMixin extends LivingEntity {

    @Shadow public abstract Iterable<ItemStack> getHandSlots();

    protected MobEntityMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "doHurtTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getFireAspect(Lnet/minecraft/world/entity/LivingEntity;)I"))
    private void enchantmentDamage(Entity target, CallbackInfoReturnable<Boolean> cir)
    {
        for (var entry : RPGDamageOverhaul.damageEnchantments.entrySet())
        {
            var enchantment = entry.getKey();
            var dc = entry.getValue().getA();
            var multiplier = entry.getValue().getB();
            int level = EnchantmentHelper.getEnchantmentLevel(enchantment, this);
            if (level > 0)
            {
                target.hurt(dc.createDamageSource(this), level * multiplier);
            }
        }
    }

    @WrapOperation(method = "doHurtTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean otherDamageAttacks(Entity target, DamageSource source, float amount, Operation<Boolean> original)
    {
        boolean ret = false;

        for (DamageClass dc : RPGDamageOverhaulAPI.getAllDamageClasses())
        {
            double dmg = getAttributeValue(dc.dmgAttribute);
            if (dmg > 0)
            {
                ret |= target.hurt(dc.createDamageSource(this), (float)dmg);
            }
        }

        double physicalDamage = getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
        double totalPhysicalDamage = getAttributeValue(Attributes.ATTACK_DAMAGE);
        for (DamageClass dc : RPGDamageOverhaulAPI.getAllDamageClasses())
        {
            if (dc.dmgAttribute == Attributes.ATTACK_DAMAGE)
                continue;
            double dmg = getAttributeBaseValue(dc.dmgAttribute);
            double totalDmg = getAttributeValue(dc.dmgAttribute);
            if (dc.isChildOf("physical"))
            {
                physicalDamage += dmg;
                totalPhysicalDamage += totalDmg;
            }
        }

        if (ModList.get().isLoaded("bettermobcombat"))
        {
            if (BetterCombatCompat.shouldBCHandleAttack(this))
            {
                return target.hurt(source, (float)totalPhysicalDamage) || ret;
            }
        }

        Map<DamageClass, Double> newDamages = new HashMap<>();
        for (ItemStack is : getHandSlots())
        {
            if (is.isEmpty() || is.getItem() == Items.AIR)
                continue;
            RPGDamageOverhaulAPI.applyItemOverrides(is, newDamages, physicalDamage);
        }

        if (newDamages.isEmpty())
            RPGDamageOverhaulAPI.applyEntityOverrides(this, newDamages, (float)totalPhysicalDamage);

        if (newDamages.isEmpty())
            return target.hurt(source, (float)totalPhysicalDamage) || ret;

        for (var entry : newDamages.entrySet())
        {
            var dc = entry.getKey();
            var dmg = entry.getValue();
            ret |= target.hurt(dc.createDamageSource(this), dmg.floatValue());
        }

        return ret;
    }


}
