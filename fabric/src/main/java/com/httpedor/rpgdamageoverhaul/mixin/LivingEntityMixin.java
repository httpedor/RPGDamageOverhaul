package com.httpedor.rpgdamageoverhaul.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.httpedor.rpgdamageoverhaul.RPGDamageOverhaulFabric;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

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

            if (OnHitEffects.increasedDamage.containsKey(this) && !OnHitEffects.increasedDamageExceptions.contains(dc))
            {
                var pair = OnHitEffects.increasedDamage.get(this);
                if (pair.getB() > System.currentTimeMillis())
                    amount = amount * pair.getA();
                else
                    OnHitEffects.increasedDamage.remove(this);
            }

            resistance = 0;
            if (dc.properties.containsKey("enchantments"))
            {
                var enchantmentsObj = dc.properties.get("enchantments").getAsJsonObject();
                if (enchantmentsObj.has("resistance"))
                {
                    for (var entry : enchantmentsObj.get("resistance").getAsJsonObject().entrySet())
                    {
                        var enchantment = instance.level().registryAccess().lookup(Registries.ENCHANTMENT).get().get(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.parse(entry.getKey())));
                        if (enchantment != null && enchantment.isPresent())
                        {
                            var multPerLevel = entry.getValue().getAsInt();
                            var level = EnchantmentHelper.getEnchantmentLevel(enchantment.get(), instance);
                            resistance += level * multPerLevel;
                        }
                    }
                }
            }
            amount = (float)(amount * (1d - resistance));
        }
        return original.call(instance, source, amount);
    }

}
