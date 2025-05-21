package com.httpedor.rpgdamageoverhaul.mixin;

import com.httpedor.rpgdamageoverhaul.RPGDamageOverhaul;
import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.DamageHandler;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.httpedor.rpgdamageoverhaul.compat.ApothicAttributesCompat;
import com.httpedor.rpgdamageoverhaul.ducktypes.DCDamageSource;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.Map;

@Mixin(value = LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Shadow protected abstract void actuallyHurt(DamageSource source, float amount);

    @Shadow public abstract double getAttributeValue(Attribute attribute);

    @Shadow public abstract double getAttributeValue(Holder<Attribute> attribute);

    @Shadow public abstract float getHealth();

    @Shadow @Nullable public abstract AttributeInstance getAttribute(Attribute p_21052_);

    @Shadow public abstract float getMaxHealth();

    @Shadow public abstract void setHealth(float p_21154_);

    @WrapOperation(method = "hurt", at = @At(value="INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z", ordinal = 3))
    private boolean noCooldown(DamageSource instance, TagKey<DamageType> tag, Operation<Boolean> original)
    {
        if (RPGDamageOverhaulAPI.isRPGDamageType(instance.type()))
            return true;
        return original.call(instance, tag);
    }


    @Inject(method = "actuallyHurt", at = @At("HEAD"), cancellable = true)
    private void damageOverrides(DamageSource source, float amount, CallbackInfo ci)
    {
        if (RPGDamageOverhaulAPI.isRPGDamageType(source.type()))
            return;

        Map<DamageClass, Double> newDcs = null;
        if (source.getDirectEntity() != null)
        {
            newDcs = RPGDamageOverhaulAPI.getEntityOverrides(source.getDirectEntity());
            if (newDcs != null && !newDcs.isEmpty())
            {
                for (var entry : newDcs.entrySet())
                    actuallyHurt(entry.getKey().createDamageSource(source.getDirectEntity(), source.getEntity()), (float) (amount * entry.getValue()));
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
                    actuallyHurt(entry.getKey(), entry.getValue().floatValue());
                ci.cancel();
            }
        }
    }

    @Inject(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/common/ForgeHooks;onLivingDamage(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;F)F", shift = At.Shift.AFTER))
    private void onHitEffects(DamageSource source, float amount, CallbackInfo ci)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.type());
        if (dc != null)
        {
            if (((DCDamageSource)source).shouldTriggerOnHitEffects())
                DamageHandler.executeOnHitEffects(dc, (LivingEntity)((Object)this), source, amount);
        }
    }

    @WrapOperation(method = "getDamageAfterArmorAbsorb", at = @At(value="INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean noDefaultArmor(DamageSource instance, TagKey<DamageType> tag, Operation<Boolean> original)
    {
        if (RPGDamageOverhaulAPI.isRPGDamageType(instance.type()))
            return true;
        return original.call(instance, tag);
    }

    @Inject(method = "getDamageAfterArmorAbsorb", at = @At("HEAD"), cancellable = true)
    private void applyDmgTypeArmor(DamageSource source, float amount, CallbackInfoReturnable<Float> cir)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.type());
        if (dc != null)
        {
            double armor = this.getAttributeValue(dc.armorAttribute) + this.getAttributeValue(Attributes.ARMOR);
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
            double armorToughness = this.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
            if (ModList.get().isLoaded("attributeslib"))
                cir.setReturnValue(ApothicAttributesCompat.applyAAArmor((LivingEntity)(Object) this, source, amount, (float)armor, (float)armorToughness));
            else
                cir.setReturnValue(CombatRules.getDamageAfterAbsorb(amount, (float)armor, (float)armorToughness));
        }
    }

    @Inject(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"))
    private void logDamage(DamageSource source, float amount, CallbackInfo ci)
    {
        //System.out.println("APPLIED DAMAGE: (" + source.type().msgId() + ", " + (source.getDirectEntity() != null ? ForgeRegistries.ENTITY_TYPES.getKey(source.getDirectEntity().getType()) : "NULL") + ", " + (source.getEntity() != null ? ForgeRegistries.ENTITY_TYPES.getKey(source.getEntity().getType()) : "NULL") + ") : " + amount + " TO " + this.getType());
    }


    @WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float applyResistances(LivingEntity instance, DamageSource source, float amount, Operation<Float> original)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.type());
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
                if (pair.getB() > System.currentTimeMillis())
                    amount = amount * pair.getA();
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
                        var enchantment = ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(entry.getKey()));
                        if (enchantment != null)
                        {
                            var multPerLevel = entry.getValue().getAsInt();
                            var level = EnchantmentHelper.getEnchantmentLevel(enchantment, (LivingEntity)(Object) this);
                            resistance += level * multPerLevel;
                        }
                    }
                }
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
            if (pair.getB() > System.currentTimeMillis())
                return getHealth() + (heal * (1 - pair.getA()));
            else
                RPGDamageOverhaul.noHealingUntil.remove(this);
        }
        return health;
    }

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void removeModifiers(CallbackInfo ci)
    {
        if (!RPGDamageOverhaul.transientModifiersDuration.containsKey(this) || level().isClientSide)
            return;

        var modifiers = RPGDamageOverhaul.transientModifiersDuration.get(this);
        for (var entry : modifiers.entrySet())
        {
            var attrId = entry.getKey();
            var duration = entry.getValue();
            if (System.currentTimeMillis() > duration)
            {
                var attr = RPGDamageOverhaul.transientModifiers.get(attrId);
                if (this.getAttribute(attr) != null)
                {
                    Float healthRatio = null;
                    if (attr == Attributes.MAX_HEALTH)
                    {
                        healthRatio = getHealth() / getMaxHealth();
                    }
                    this.getAttribute(attr).removeModifier(attrId);
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
