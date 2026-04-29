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

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {

    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Shadow
    protected abstract void actuallyHurt(DamageSource source, float amount);

    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getFireAspect(Lnet/minecraft/world/entity/LivingEntity;)I"))
    private void enchantmentDamage(Entity target, CallbackInfo ci)
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

    @WrapOperation(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean otherDamageAttacks(Entity target, DamageSource source, float amount, Operation<Boolean> original, @Local CriticalHitEvent che)
    {
        boolean ret = false;
        for (DamageClass dc : RPGDamageOverhaulAPI.getAllDamageClasses())
        {
            double dmg = getAttributeValue(dc.dmgAttribute);
            if (dmg > 0)
            {
                if (che != null)
                    dmg *= che.getDamageModifier();
                ret |= target.hurt(dc.createDamageSource(this), (float)dmg);
            }
        }
        if (ModList.get().isLoaded("bettercombat"))
        {
            if (BetterCombatCompat.shouldBCHandleAttack((Player)(Object)this))
                return original.call(target, source, amount) || ret;
        }

        var is = getMainHandItem();
        Map<DamageClass, Double> newDamages = new HashMap<>();
        float originalItemDamage = 0;
        for (var mod : is.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE))
            originalItemDamage += mod.getAmount();
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
                if (che != null)
                    dmg *= che.getDamageModifier();
                ret |= target.hurt(dc.createDamageSource(this), dmg);
            }
            return ret;
        }
    }

    @Inject(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setHealth(F)V"))
    private void logDamage(DamageSource source, float amount, CallbackInfo ci)
    {
        //System.out.println("APPLIED DAMAGE: (" + source.type().msgId() + ", " + (source.getDirectEntity() != null ? ForgeRegistries.ENTITY_TYPES.getKey(source.getDirectEntity().getType()) : "NULL") + ", " + (source.getEntity() != null ? ForgeRegistries.ENTITY_TYPES.getKey(source.getEntity().getType()) : "NULL") + ") : " + amount + " TO " + this.getType());
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

    @WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float applyResistances(Player instance, DamageSource source, float amount, Operation<Float> original)
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

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void removeModifiers(CallbackInfo ci)
    {
        if (!RPGDamageOverhaul.transientModifiersDuration.containsKey(this) || level().isClientSide)
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
