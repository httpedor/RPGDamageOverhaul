package com.httpedor.rpgdamageoverhaul.mixin.bettermobcombat;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.EntityPlayer_BetterCombat;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.damage.DamageType;
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

    @Shadow protected abstract DamageSource create(RegistryKey<DamageType> p_270142_, @Nullable Entity p_270696_);

    @Inject(method = "mobAttack", at = @At("RETURN"), cancellable = true)
    public void onMobAttack(LivingEntity mob, CallbackInfoReturnable<DamageSource> cir)
    {
        if (!FabricLoader.getInstance().isModLoaded("bettermobcombat"))
            return;

        EntityPlayer_BetterCombat attacker = (EntityPlayer_BetterCombat) mob;
        AttackHand attackHand = attacker.getCurrentAttack();
        if (attackHand != null)
        {
            DamageClass[] overrides = RPGDamageOverhaulAPI.getBetterCombatAttackOverrides(Registries.ITEM.getId(attackHand.itemStack().getItem()));
            if (overrides != null)
            {
                int combo = attackHand.combo().current()-1;
                if (combo < overrides.length)
                    cir.setReturnValue(create(overrides[combo].damageType, mob));
            }
        }
    }


}
