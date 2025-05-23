package com.httpedor.rpgdamageoverhaul.mixin;

import com.google.gson.JsonPrimitive;
import com.httpedor.rpgdamageoverhaul.RPGDamageOverhaul;
import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.DamageHandler;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.httpedor.rpgdamageoverhaul.ducktypes.DCDamageSource;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow protected abstract void applyDamage(DamageSource source, float amount);

    @Shadow public abstract double getAttributeValue(EntityAttribute attribute);

    @Shadow public abstract float getHealth();

    @Shadow @Nullable public abstract EntityAttributeInstance getAttributeInstance(EntityAttribute attribute);

    @Shadow public abstract float getMaxHealth();

    @Shadow public abstract void setHealth(float health);

    @WrapOperation(method = "damage", at = @At(value="INVOKE", target = "Lnet/minecraft/entity/damage/DamageSource;isIn(Lnet/minecraft/registry/tag/TagKey;)Z", ordinal = 3))
    private boolean noCooldown(DamageSource instance, TagKey<DamageType> tag, Operation<Boolean> original)
    {
        if (RPGDamageOverhaulAPI.isRPGDamageType(instance.getType()))
            return true;
        return original.call(instance, tag);
    }


    @Inject(method = "applyDamage", at = @At("HEAD"), cancellable = true)
    private void damageOverrides(DamageSource source, float amount, CallbackInfo ci)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.getType());
        if (dc != null)
        {
            if (((DCDamageSource)source).shouldTriggerOnHitEffects())
                DamageHandler.executeOnHitEffects(dc, (LivingEntity)((Object)this), source, amount);

            return;
        }

        var newDamages = DamageHandler.applyDamageOverrides((LivingEntity)(Object)this, source, amount);
        if (newDamages != null)
        {
            for (Map.Entry<DamageSource, Double> entry : newDamages.entrySet())
                applyDamage(entry.getKey(), entry.getValue().floatValue());
            ci.cancel();
        }

    }

    @WrapOperation(method = "applyArmorToDamage", at = @At(value="INVOKE", target = "Lnet/minecraft/entity/damage/DamageSource;isIn(Lnet/minecraft/registry/tag/TagKey;)Z"))
    private boolean noDefaultArmor(DamageSource instance, TagKey<DamageType> tag, Operation<Boolean> original)
    {
        if (RPGDamageOverhaulAPI.isRPGDamageType(instance.getType()))
            return true;
        return original.call(instance, tag);
    }

    @Inject(method = "applyArmorToDamage", at = @At("HEAD"), cancellable = true)
    private void applyDmgTypeArmor(DamageSource source, float amount, CallbackInfoReturnable<Float> cir)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.getType());
        if (dc != null)
        {
            if (dc.properties.getOrDefault("ignoreArmor", new JsonPrimitive(false)).getAsBoolean())
            {
                cir.setReturnValue(amount);
                return;
            }
            double armor = this.getAttributeValue(dc.armorAttribute) + this.getAttributeValue(EntityAttributes.GENERIC_ARMOR);
            DamageClass parent = RPGDamageOverhaulAPI.getDamageClass(dc.parentName);
            while (parent != null)
            {
                armor += this.getAttributeValue(parent.armorAttribute);
                parent = RPGDamageOverhaulAPI.getDamageClass(parent.parentName);
            }
            double armorToughness = this.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS);
            cir.setReturnValue(DamageUtil.getDamageLeft(amount, (float)armor, (float)armorToughness));
        }
    }

    @Inject(method = "applyDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;setHealth(F)V"))
    private void logDamage(DamageSource source, float amount, CallbackInfo ci)
    {
        System.out.println("APPLIED DAMAGE: " + source + " : " + amount + " TO " + this);
    }


    @WrapOperation(method = "applyDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;modifyAppliedDamage(Lnet/minecraft/entity/damage/DamageSource;F)F"))
    private float applyResistances(LivingEntity instance, DamageSource source, float amount, Operation<Float> original)
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
        }
        return original.call(instance, source, amount);
    }

    @ModifyVariable(method = "setHealth", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float applyHealBlock(float health)
    {
        if (health > getHealth() && RPGDamageOverhaul.noHealingUntil.containsKey(this))
        {
            var heal = health - getHealth();
            var pair = RPGDamageOverhaul.noHealingUntil.get(this);
            if (pair.getRight() > System.currentTimeMillis())
                return getHealth() + (heal * (1 - pair.getLeft()));
            else
                RPGDamageOverhaul.noHealingUntil.remove(this);
        }
        if (health < getHealth() && RPGDamageOverhaul.increasedDamage.containsKey(this))
        {
            var dmg = getHealth() - health;
            var pair = RPGDamageOverhaul.increasedDamage.get(this);
            if (pair.getRight() > System.currentTimeMillis())
                return getHealth() - (dmg * pair.getLeft());
            else
                RPGDamageOverhaul.increasedDamage.remove(this);
        }

        return health;
    }

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void removeModifiers(CallbackInfo ci)
    {
        if (!RPGDamageOverhaul.transientModifiersDuration.containsKey(this) || getWorld().isClient)
            return;

        var modifiers = RPGDamageOverhaul.transientModifiersDuration.get(this);
        for (var entry : modifiers.entrySet().stream().toList())
        {
            var attrId = entry.getKey();
            var duration = entry.getValue();
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
