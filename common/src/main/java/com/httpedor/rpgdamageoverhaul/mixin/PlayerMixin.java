package com.httpedor.rpgdamageoverhaul.mixin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.httpedor.rpgdamageoverhaul.AttributeUtils;
import com.httpedor.rpgdamageoverhaul.SharedLogic;
import com.httpedor.rpgdamageoverhaul.damageproperties.onhit.ModifyDamageOnHit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.httpedor.rpgdamageoverhaul.api.DamageClass.DCAttribute;
import com.httpedor.rpgdamageoverhaul.compat.BetterCombatCompat;
import com.httpedor.rpgdamageoverhaul.compat.GearSocketsCompat;
import com.httpedor.rpgdamageoverhaul.damageproperties.EnchantmentAttributesProperty;
import com.httpedor.rpgdamageoverhaul.damageproperties.onhit.AttributeModifierOnHit;
import com.httpedor.rpgdamageoverhaul.ducktypes.DCDamageSource;
import com.httpedor.rpgdamageoverhaul.platform.Services;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {

    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Shadow
    protected abstract void actuallyHurt(DamageSource source, float amount);

    @WrapOperation(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean otherDamageAttacks(Entity target, DamageSource source, float amount, Operation<Boolean> original, @Local(ordinal = 2) boolean isCrit)
    {
        boolean ret = false;
        for (DamageClass dc : RPGDamageOverhaulAPI.getAllDamageClasses())
        {
            double dmg = SharedLogic.getAttributeValue(dc, DCAttribute.DAMAGE, this);
            if (isCrit)
                dmg *= 1.5;
            if (dmg > 0)
                ret |= target.hurt(dc.createDamageSource(this), (float)dmg);
        }
        var is = getMainHandItem();
        Map<DamageClass, Double> newDamages = new HashMap<>();
        float originalItemDamage = 0;
        List<Double> mods = new ArrayList<>();
        is.forEachModifier(EquipmentSlot.MAINHAND, (attr, mod) -> {
            if (attr.equals(Attributes.ATTACK_DAMAGE))
                mods.add(mod.amount());
        });
        for (var mod : mods)
            originalItemDamage += mod.floatValue();

        // GearSockets compat (no-op when the mod is absent): sockets can convert part of the swing into
        // elemental damage. The converted portion is dealt as its own damage class and removed from the
        // physical pool below, so the target's per-class armor/resistance applies to each piece. Done before the
        // Better Combat hand-off so the conversion still happens (and the reduced amount is passed on) when
        // Better Combat takes over the physical part of the swing.
        if (GearSocketsCompat.isEnabled())
        {
            Map<DamageClass, Float> conversions = new HashMap<>();
            float converted = GearSocketsCompat.convertDamage(is, originalItemDamage, amount, conversions);
            if (converted > 0)
            {
                amount -= converted;
                for (var entry : conversions.entrySet())
                {
                    float dmg = entry.getValue();
                    if (isCrit)
                        dmg *= 1.5f;
                    if (dmg > 0)
                        ret |= target.hurt(entry.getKey().createDamageSource(this), dmg);
                }
            }
        }

        // The attacker's own damage_conversion attributes turn part of the physical swing into elemental damage,
        // dealt per class so the target's armor/resistance applies to each piece, then removed from the pool.
        Map<DamageClass, Float> attrConversions = new HashMap<>();
        float attrConverted = SharedLogic.applyDamageConversions(this, amount, attrConversions);
        if (attrConverted > 0)
        {
            amount -= attrConverted;
            for (var entry : attrConversions.entrySet())
            {
                float dmg = entry.getValue();
                if (isCrit)
                    dmg *= 1.5f;
                if (dmg > 0)
                    ret |= target.hurt(entry.getKey().createDamageSource(this), dmg);
            }
        }

        if (Services.PLATFORM.isModLoaded("bettercombat"))
        {
            if (BetterCombatCompat.shouldBCHandleAttack((Player)(Object)this))
                return original.call(target, source, amount) || ret;
        }

        float dmgFromOtherSources = amount - originalItemDamage; // If the original damage is different from the amount, it means that some other mod (enchantment, potion, etc) is applying damage in the attack method. We need to take that into account when applying item overrides, to avoid overwriting those mods.
        RPGDamageOverhaulAPI.applyItemOverrides(is, newDamages, dmgFromOtherSources);
        if (newDamages.isEmpty())
        {
            DamageClass blunt = RPGDamageOverhaulAPI.getDamageClass("blunt");
            if (blunt != null)
                return target.hurt(blunt.createDamageSource(this), amount) || ret;
            else
                return original.call(target, source, amount) || ret;
        }
        else
        {
            for (var entry : newDamages.entrySet())
            {
                var dc = entry.getKey();
                var dmg = entry.getValue().floatValue();
                if (isCrit)
                    dmg *= 1.5;
                ret |= target.hurt(dc.createDamageSource(this), dmg);
            }
            return ret;
        }
    }

    @Inject(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setHealth(F)V"))
    private void logDamage(DamageSource source, float amount, CallbackInfo ci, @Local(ordinal = 1) float damageAmount)
    {
        //System.out.println("APPLIED DAMAGE: (" + source.type().msgId() + ", " + (source.getDirectEntity() != null ? source.getDirectEntity().getType() : "NULL") + ", " + (source.getEntity() != null ? source.getEntity().getType() : "NULL") + ") : " + amount + " TO " + this.getType());
    }

    @ModifyVariable(method = "actuallyHurt", at = @At(value = "HEAD"), argsOnly = true)
    private float modifyDamage(float amount, @Local(argsOnly = true) DamageSource source)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.type());
        if (dc != null)
            return SharedLogic.modifyDamagePreArmor(amount, this, dc);
        return amount;
    }

    @Inject(method = "actuallyHurt", at = @At("HEAD"), cancellable = true)
    private void damageOverrides(DamageSource source, float amount, CallbackInfo ci)
    {
        if (RPGDamageOverhaulAPI.isRPGDamageType(source.typeHolder()))
            return;

        Map<DamageClass, Double> newDcs = null;
        if (source.getDirectEntity() != null)
        {
            newDcs = RPGDamageOverhaulAPI.getEntityOverrides(source.getDirectEntity());
            if (newDcs != null && !newDcs.isEmpty())
            {
                for (var entry : newDcs.entrySet())
                {
                    var dc = entry.getKey();
                    float newDamage = (float) (amount * entry.getValue());
                    // Add the entity's damage attribute to the damage, that way, if the entity has stats like
                    // "+20% frost damage", it will also apply to projectile damages
                    // Removed because RPGDO is made in a way that only interacts with other mods' attributes. If a mod
                    //doesn't add an attribute for something, it's probably balanced in a way that would be broken if that
                    //damage was changed. The only exception for this is when we change specific stuff with overrides,
                    //but that's allowed because I know the numbers I'm changing. While here, we might screw up some other
                    //mod's balance if we change stuff too much.
                    /*if (source.getEntity() != null && source.getEntity() instanceof LivingEntity le)
                    {
                        var attrInstance = le.getAttribute(dc.getAttribute(DCAttribute.DAMAGE));
                        if (attrInstance != null)
                            newDamage = AttributeUtils.applyAttributeModifiers(newDamage, attrInstance.getModifiers());
                    }*/
                    actuallyHurt(dc.createDamageSource(source.getDirectEntity(), source.getEntity()), newDamage);
                }
                ci.cancel();
                return;
            }
        }

        if (newDcs == null || newDcs.isEmpty())
        {
            var newDamages = RPGDamageOverhaulAPI.applyDamageOverrides(source, amount);
            if (newDamages != null && !newDamages.isEmpty())
            {
                for (Map.Entry<DamageSource, Double> entry : newDamages.entrySet())
                {
                    actuallyHurt(entry.getKey(), entry.getValue().floatValue());
                }
                ci.cancel();
            }
        }
    }

    @Inject(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setHealth(F)V"))
    private void onHitEffects(DamageSource source, float amount, CallbackInfo ci)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.type());
        if (dc != null)
        {
            if (((DCDamageSource)source).shouldTriggerOnHitEffects())
                dc.applyOnHitEffects((LivingEntity)(Object)this, source, amount);
        }
    }

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void removeModifiers(CallbackInfo ci)
    {
        if (level().isClientSide)
            return;

        AttributeModifierOnHit.pruneExpired((LivingEntity)(Object)this);
    }
}
