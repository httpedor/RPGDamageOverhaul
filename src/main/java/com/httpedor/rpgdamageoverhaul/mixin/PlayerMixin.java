package com.httpedor.rpgdamageoverhaul.mixin;

import com.httpedor.rpgdamageoverhaul.RPGDamageOverhaul;
import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.DamageHandler;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.httpedor.rpgdamageoverhaul.compat.BetterCombatCompat;
import com.httpedor.rpgdamageoverhaul.ducktypes.DCDamageSource;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
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
    private boolean otherDamageAttacks(Entity target, DamageSource source, float amount, Operation<Boolean> original, @Local(ordinal = 2) boolean isCrit)
    {
        boolean ret = false;
        for (DamageClass dc : RPGDamageOverhaulAPI.getAllDamageClasses())
        {
            double dmg = getAttributeValue(dc.dmgAttribute);
            if (dmg > 0)
            {
                if (isCrit)
                    dmg *= 1.5;
                ret |= target.damage(dc.createDamageSource(this), (float)dmg);
            }
        }
        if (FabricLoader.getInstance().isModLoaded("bettercombat"))
        {
            if (BetterCombatCompat.shouldBCHandleAttack(this))
                return original.call(target, source, amount) || ret;
        }

        var is = getMainHandStack();
        Map<DamageClass, Double> newDamages = new HashMap<>();
        float originalItemDamage = 0;
        for (var mod : is.getAttributeModifiers(EquipmentSlot.MAINHAND).get(EntityAttributes.GENERIC_ATTACK_DAMAGE))
            originalItemDamage += mod.getValue();
        float dmgFromOtherSources = amount - originalItemDamage; // If the original damage is different from the amount, it means that some other mod (enchantment, potion, etc) is applying damage in the attack method. We need to take that into account when applying item overrides, to avoid overwriting those mods.
        RPGDamageOverhaulAPI.applyItemOverrides(is, newDamages, dmgFromOtherSources);
        if (newDamages.isEmpty())
        {
            DamageClass blunt = RPGDamageOverhaulAPI.getDamageClass("blunt");
            if (blunt != null)
                return target.damage(blunt.createDamageSource(this), amount) || ret;
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
                ret |= target.damage(dc.createDamageSource(this), dmg);
            }
            return ret;
        }
    }


    @Inject(method = "applyDamage", at = @At("HEAD"), cancellable = true)
    private void damageOverrides(DamageSource source, float amount, CallbackInfo ci)
    {
        if (RPGDamageOverhaulAPI.isRPGDamageType(source.getTypeRegistryEntry()))
            return;

        Map<DamageClass, Double> newDcs = null;
        if (source.getSource() != null)
        {
            newDcs = RPGDamageOverhaulAPI.getEntityOverrides(source.getSource());
            if (newDcs != null && !newDcs.isEmpty())
            {
                for (var entry : newDcs.entrySet())
                    applyDamage(entry.getKey().createDamageSource(source.getSource(), source.getAttacker()), (float) (amount * entry.getValue()));
                ci.cancel();
                return;
            }
        }

        if (newDcs == null || newDcs.isEmpty())
        {
            var newDamages = DamageHandler.applyDamageOverrides((LivingEntity)(Object)this, source, amount);
            if (newDamages != null && !newDamages.isEmpty())
            {
                for (Map.Entry<DamageSource, Double> entry : newDamages.entrySet())
                    applyDamage(entry.getKey(), entry.getValue().floatValue());
                ci.cancel();
            }
        }
    }

    @Inject(method = "applyDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setHealth(F)V"))
    private void onHitEffects(DamageSource source, float amount, CallbackInfo ci)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.getType());
        if (dc != null)
        {
            if (((DCDamageSource)source).shouldTriggerOnHitEffects())
                DamageHandler.executeOnHitEffects(dc, (LivingEntity)((Object)this), source, amount);
        }
    }

    @WrapOperation(method = "applyDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;modifyAppliedDamage(Lnet/minecraft/entity/damage/DamageSource;F)F"))
    private float applyResistances(PlayerEntity instance, DamageSource source, float amount, Operation<Float> original)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.getType());
        if (dc != null)
        {
            double resistance = this.getAttributeValue(dc.resistanceAttribute);
            DamageClass parent = RPGDamageOverhaulAPI.getDamageClass(dc.parentName);
            while (parent != null)
            {
                resistance += this.getAttributeValue(parent.resistanceAttribute);
                parent = RPGDamageOverhaulAPI.getDamageClass(parent.parentName);
            }
            amount = (float)(amount * (1d - resistance));

            if (RPGDamageOverhaul.increasedDamage.containsKey(this) && !RPGDamageOverhaul.increasedDamageExceptions.contains(dc))
            {
                var pair = RPGDamageOverhaul.increasedDamage.get(this);
                if (pair.getRight() > System.currentTimeMillis())
                    amount = amount * pair.getLeft();
                else
                    RPGDamageOverhaul.increasedDamage.remove(this);
            }

            resistance = 0;
            if (dc.properties.containsKey("enchantments"))
            {
                var enchantmentsObj = dc.properties.get("enchantments").getAsJsonObject();
                if (enchantmentsObj.has("resistance"))
                {
                    for (var entry : enchantmentsObj.get("resistance").getAsJsonObject().entrySet())
                    {
                        var enchantment = Registries.ENCHANTMENT.get(new Identifier(entry.getKey()));
                        if (enchantment != null)
                        {
                            var multPerLevel = entry.getValue().getAsInt();
                            var level = EnchantmentHelper.getEquipmentLevel(enchantment, (LivingEntity)(Object) this);
                            resistance += level * multPerLevel;
                        }
                    }
                }
            }
            amount = (float)(amount * (1d - resistance));
        }
        return original.call(instance, source, amount);
    }

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void removeModifiers(CallbackInfo ci)
    {
        if (!RPGDamageOverhaul.transientModifiersDuration.containsKey(this) || getWorld().isClient)
            return;

        var modifiers = RPGDamageOverhaul.transientModifiersDuration.get(this);
        var keys = modifiers.keySet().toArray(new java.util.UUID[0]);
        for (var key : keys)
        {
            var attrId = key;
            var duration = modifiers.get(key);
            if (System.currentTimeMillis() > duration)
            {
                var attr = RPGDamageOverhaul.transientModifiers.get(attrId);
                if (this.getAttributeInstance(attr) != null)
                {
                    Float healthRatio = null;
                    if (attr == EntityAttributes.GENERIC_MAX_HEALTH)
                    {
                        healthRatio = getHealth() / getMaxHealth();
                    }
                    this.getAttributeInstance(attr).removeModifier(attrId);
                    if (healthRatio != null)
                        setHealth(getMaxHealth() * healthRatio);
                }
                modifiers.remove(attrId);
                RPGDamageOverhaul.transientModifiers.remove(attrId);
            }
        }
        if (modifiers.isEmpty())
            RPGDamageOverhaul.transientModifiersDuration.remove(this);
    }
}
