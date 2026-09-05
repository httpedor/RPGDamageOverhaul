package com.httpedor.rpgdamageoverhaul.mixin;

import java.util.Stack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.httpedor.rpgdamageoverhaul.SharedLogic;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.damagesource.DamageContainer;

/**
 * Per-class absorption for non-player living entities, applied at the vanilla absorption stage of {@code
 * actuallyHurt} (just before {@code onLivingDamagePre}), so it soaks damage after armor and resistance and just
 * like vanilla absorption. Players re-implement {@code actuallyHurt} instead of calling {@code super}, so they are
 * handled by their own {@link PlayerAbsorptionMixin}.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityAbsorptionMixin {
    @Shadow protected Stack<DamageContainer> damageContainers;

    @Inject(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/common/CommonHooks;onLivingDamagePre(Lnet/minecraft/world/entity/LivingEntity;Lnet/neoforged/neoforge/common/damagesource/DamageContainer;)F"))
    private void rpgdamageoverhaul$applyClassAbsorption(DamageSource source, float amount, CallbackInfo ci)
    {
        LivingEntity self = (LivingEntity)(Object)this;
        if (self.level().isClientSide)
            return;
        DamageContainer container = damageContainers.peek();
        float remaining = container.getNewDamage();
        float absorbed = SharedLogic.consumeAbsorption(self, source, remaining);
        if (absorbed > 0f)
            container.setNewDamage(remaining - absorbed);
    }
}
