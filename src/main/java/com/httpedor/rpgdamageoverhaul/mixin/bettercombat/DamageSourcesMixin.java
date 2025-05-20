package com.httpedor.rpgdamageoverhaul.mixin.bettercombat;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.EntityPlayer_BetterCombat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DamageSources.class)
public abstract class DamageSourcesMixin {

    @Shadow public abstract DamageSource create(RegistryKey<DamageType> key, @Nullable Entity attacker);

    @Inject(method = "playerAttack", at = @At("RETURN"), cancellable = true)
    public void onPlayerAttack(PlayerEntity attacker, CallbackInfoReturnable<DamageSource> cir) {
        AttackHand attackHand = ((EntityPlayer_BetterCombat) attacker).getCurrentAttack();
        if (attackHand != null)
        {
            DamageClass[] attackOverrides = RPGDamageOverhaulAPI.getBetterCombatAttackOverrides(Registries.ITEM.getId(attackHand.itemStack().getItem()));
            if (attackOverrides != null)
            {

                int combo = attackHand.combo().current()-1;
                if (combo < attackOverrides.length)
                    cir.setReturnValue(create(attackOverrides[combo].damageType, attacker));
            }
        }
    }

}
