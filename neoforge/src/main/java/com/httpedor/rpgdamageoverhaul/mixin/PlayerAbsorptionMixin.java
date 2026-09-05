package com.httpedor.rpgdamageoverhaul.mixin;

import java.util.Stack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.httpedor.rpgdamageoverhaul.SharedLogic;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.damagesource.DamageContainer;

/**
 * Per-class absorption for players. Separate from {@link LivingEntityAbsorptionMixin} because {@link Player}
 * overrides {@code actuallyHurt} rather than calling {@code super}, so the LivingEntity injection never runs for it.
 * Extends {@link LivingEntity} so the inherited {@code damageContainers} field can be shadowed, mirroring
 * {@code PlayerResistanceMixin}.
 */
@Mixin(Player.class)
public abstract class PlayerAbsorptionMixin extends LivingEntity {
    protected PlayerAbsorptionMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/common/CommonHooks;onLivingDamagePre(Lnet/minecraft/world/entity/LivingEntity;Lnet/neoforged/neoforge/common/damagesource/DamageContainer;)F"))
    private void rpgdamageoverhaul$applyClassAbsorption(DamageSource source, float amount, CallbackInfo ci)
    {
        if (level().isClientSide)
            return;
        DamageContainer container = damageContainers.peek();
        float remaining = container.getNewDamage();
        float absorbed = SharedLogic.consumeAbsorption((LivingEntity)(Object)this, source, remaining);
        if (absorbed > 0f)
            container.setNewDamage(remaining - absorbed);
    }
}
