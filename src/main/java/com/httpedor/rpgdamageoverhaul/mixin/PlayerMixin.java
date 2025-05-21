package com.httpedor.rpgdamageoverhaul.mixin;

import com.httpedor.rpgdamageoverhaul.RPGDamageOverhaul;
import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.DamageHandler;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.httpedor.rpgdamageoverhaul.compat.BetterCombatCompat;
import com.httpedor.rpgdamageoverhaul.ducktypes.DCDamageSource;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
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
        if (ModList.get().isLoaded("bettercombat"))
        {
            if (BetterCombatCompat.shouldBCHandleAttack((Player)(Object)this))
                return original.call(target, source, amount) || ret;
        }

        var is = getMainHandItem();
        Map<DamageClass, Double> newDamages = new HashMap<>();
        RPGDamageOverhaulAPI.applyItemOverrides(is, newDamages);
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
                var dmg = entry.getValue();
                ret |= target.hurt(dc.createDamageSource(this), dmg.floatValue());
            }
            return ret;
        }
    }

    @Inject(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setHealth(F)V"))
    private void logDamage(DamageSource source, float amount, CallbackInfo ci)
    {
        System.out.println("APPLIED DAMAGE: (" + source.type().msgId() + ", " + (source.getDirectEntity() != null ? ForgeRegistries.ENTITY_TYPES.getKey(source.getDirectEntity().getType()) : "NULL") + ", " + (source.getEntity() != null ? ForgeRegistries.ENTITY_TYPES.getKey(source.getEntity().getType()) : "NULL") + ") : " + amount + " TO " + this.getType());
    }


    @Inject(method = "actuallyHurt", at = @At("HEAD"), cancellable = true)
    private void damageOverrides(DamageSource source, float amount, CallbackInfo ci)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.type());
        if (dc != null)
        {
            if (((DCDamageSource)source).shouldTriggerOnHitEffects())
                DamageHandler.executeOnHitEffects(dc, this, source, amount);

            return;
        }

        var newDamages = DamageHandler.applyDamageOverrides(this, source, amount);
        if (newDamages != null)
        {
            for (Map.Entry<DamageSource, Double> entry : newDamages.entrySet())
                actuallyHurt(entry.getKey(), entry.getValue().floatValue());
            ci.cancel();
        }
    }
}
