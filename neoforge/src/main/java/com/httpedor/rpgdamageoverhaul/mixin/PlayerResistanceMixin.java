package com.httpedor.rpgdamageoverhaul.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import com.httpedor.rpgdamageoverhaul.SharedLogic;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

// This needs to be sepparate from common because NeoForge adds some stuff to the actuallyHurt method
@Mixin(Player.class)
public abstract class PlayerResistanceMixin extends LivingEntity {
    protected PlayerResistanceMixin(EntityType<? extends LivingEntity> entityType, Level level) {
		super(entityType, level);
	}

	@ModifyVariable(method = "actuallyHurt", at = @At("STORE"), ordinal = 3)
    private float modifyDamage(float damage, @Local(argsOnly = true) DamageSource source)
    {
        return SharedLogic.getDamagePostResistance(damage, this, source);
    }
}
