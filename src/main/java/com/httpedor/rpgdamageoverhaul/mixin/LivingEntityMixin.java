package com.httpedor.rpgdamageoverhaul.mixin;

import com.httpedor.rpgdamageoverhaul.RPGDamageOverhaul;
import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.DamageHandler;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.httpedor.rpgdamageoverhaul.ducktypes.DCDamageSource;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.util.Identifier;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.Registries;
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

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    private Map<DamageClass, Long> rpgdamageoverhaul$env_iframes = new HashMap<>();
    private WeakHashMap<Entity, Long> rpgdamageoverhaul$ent_iframes = new WeakHashMap<>();

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow protected abstract void applyDamage(DamageSource source, float amount);

    @Shadow public abstract double getAttributeValue(EntityAttribute attribute);

    @Shadow public abstract float getHealth();

    @Shadow @Nullable public abstract EntityAttributeInstance getAttributeInstance(EntityAttribute attribute);

    @Shadow public abstract float getMaxHealth();

    @Shadow public abstract void setHealth(float health);

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        var sup = super.isInvulnerableTo(source);
        if (sup || !RPGDamageOverhaulAPI.isRPGDamageType(source.getTypeRegistryEntry()))
            return sup;
        var ent = source.getSource();
        if (ent != null)
        {
            var until = rpgdamageoverhaul$ent_iframes.get(ent);
            if (until != null && until >= getWorld().getTime() && until - 10 != getWorld().getTime()) // -10 because same-tick damage should not be ignored.
            {
                return true;
            }
            rpgdamageoverhaul$ent_iframes.put(ent, getWorld().getTime()+10);
        }
        else
        {
            var dc = RPGDamageOverhaulAPI.getDamageClass(source.getType());
            var until = rpgdamageoverhaul$env_iframes.get(dc);
            if (until != null && until >= getWorld().getTime() && until - 10 != getWorld().getTime())
            {
                return true;
            }
            rpgdamageoverhaul$env_iframes.put(dc, getWorld().getTime()+10);
        }

        return sup;
    }

    @WrapOperation(method = "damage", at = @At(value="INVOKE", target = "Lnet/minecraft/entity/damage/DamageSource;isIn(Lnet/minecraft/registry/tag/TagKey;)Z", ordinal = 3))
    private boolean noCooldown(DamageSource instance, TagKey<DamageType> tag, Operation<Boolean> original)
    {
        if (RPGDamageOverhaulAPI.isRPGDamageType(instance.getTypeRegistryEntry()))
            return true;
        return original.call(instance, tag);
    }


    @Inject(method = "applyDamage", at = @At("HEAD"), cancellable = true)
    private void damageOverrides(DamageSource source, float amount, CallbackInfo ci)
    {
        if (RPGDamageOverhaulAPI.isRPGDamageType(source.getTypeRegistryEntry()))
            return;

        // Entity overrides
        // Only for projectiles, as melee attacks will already deal RPGDO damage through the doHurtTarget injection at MobEntityMixin
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

        // DamageType overrides
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

    @Inject(method = "applyDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;setHealth(F)V"))
    private void onHitEffects(DamageSource source, float amount, CallbackInfo ci)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.getType());
        if (dc != null)
        {
            if (((DCDamageSource)source).shouldTriggerOnHitEffects())
                DamageHandler.executeOnHitEffects(dc, (LivingEntity)((Object)this), source, amount);
        }
    }

    @WrapOperation(method = "applyArmorToDamage", at = @At(value="INVOKE", target = "Lnet/minecraft/entity/damage/DamageSource;isIn(Lnet/minecraft/registry/tag/TagKey;)Z"))
    private boolean noDefaultArmor(DamageSource instance, TagKey<DamageType> tag, Operation<Boolean> original)
    {
        if (RPGDamageOverhaulAPI.isRPGDamageType(instance.getTypeRegistryEntry()))
            return true;
        return original.call(instance, tag);
    }

    @Inject(method = "applyArmorToDamage", at = @At("HEAD"), cancellable = true)
    private void applyDmgTypeArmor(DamageSource source, float amount, CallbackInfoReturnable<Float> cir)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.getType());
        if (dc != null)
        {
            double armor = this.getAttributeValue(dc.armorAttribute) + (this.getAttributeValue(EntityAttributes.GENERIC_ARMOR) * 0.8);
            DamageClass parent = RPGDamageOverhaulAPI.getDamageClass(dc.parentName);
            while (parent != null)
            {
                armor += this.getAttributeValue(parent.armorAttribute);
                parent = RPGDamageOverhaulAPI.getDamageClass(parent.parentName);
            }
            if (dc.properties.containsKey("armorEffectiveness"))
            {
                double effectiveness = dc.properties.get("armorEffectiveness").getAsDouble();
                armor *= effectiveness;
            }
            double armorToughness = this.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS);
            cir.setReturnValue(DamageUtil.getDamageLeft(amount, (float)armor, (float)armorToughness));
        }
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

    @SuppressWarnings("unlikely-arg-type")
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
        return health;
    }

    @SuppressWarnings("unlikely-arg-type")
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
