package com.httpedor.rpgdamageoverhaul.mixin;

import java.util.HashMap;
import java.util.Map;

import com.httpedor.rpgdamageoverhaul.SharedLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.httpedor.rpgdamageoverhaul.api.DamageClass.DCAttribute;
import com.httpedor.rpgdamageoverhaul.compat.BetterCombatCompat;
import com.httpedor.rpgdamageoverhaul.damageproperties.EnchantmentAttributesProperty;
import com.httpedor.rpgdamageoverhaul.platform.Services;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryDataLoader;
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

@Mixin(Mob.class)
public abstract class MobEntityMixin extends LivingEntity {

    @Shadow public abstract Iterable<ItemStack> getHandSlots();

    protected MobEntityMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @WrapOperation(method = "doHurtTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean otherDamageAttacks(Entity target, DamageSource source, float amount, Operation<Boolean> original)
    {
        boolean ret = false;

        for (DamageClass dc : RPGDamageOverhaulAPI.getAllDamageClasses())
        {
            double dmg = SharedLogic.getAttributeValue(dc, DCAttribute.DAMAGE, this);
            if (dmg > 0)
            {
                ret |= target.hurt(dc.createDamageSource(this), (float)dmg);
            }
        }

        double physicalDamage = getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
        double totalPhysicalDamage = getAttributeValue(Attributes.ATTACK_DAMAGE);
        for (DamageClass dc : RPGDamageOverhaulAPI.getAllDamageClasses())
        {
            var dmgAttr = dc.getAttribute(DCAttribute.DAMAGE);
            if (dmgAttr == Attributes.ATTACK_DAMAGE)
                continue;
            // Mobs loaded before this damage class was injected into their entity type's supplier don't have the
            // attribute at all, and reading it would throw instead of returning nothing.
            if (!getAttributes().hasAttribute(dmgAttr))
                continue;
            double dmg = getAttributeBaseValue(dmgAttr);
            double totalDmg = getAttributeValue(dmgAttr);
            if (dc.isChildOf("physical"))
            {
                physicalDamage += dmg;
                totalPhysicalDamage += totalDmg;
            }
        }

        // The attacker's own damage_conversion attributes turn part of the physical swing into elemental damage,
        // dealt per class so the target's armor/resistance applies to each piece, then removed from the pool.
        Map<DamageClass, Float> attrConversions = new HashMap<>();
        float attrConverted = SharedLogic.applyDamageConversions(this, (float)totalPhysicalDamage, attrConversions);
        if (attrConverted > 0)
        {
            totalPhysicalDamage -= attrConverted;
            for (var entry : attrConversions.entrySet())
            {
                float dmg = entry.getValue();
                if (dmg > 0)
                    ret |= target.hurt(entry.getKey().createDamageSource(this), dmg);
            }
        }

        if (Services.PLATFORM.isModLoaded("bettermobcombat"))
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
