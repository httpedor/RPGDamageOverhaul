package com.httpedor.rpgdamageoverhaul.mixin.bettermobcombat;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.EntityPlayer_BetterCombat;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DamageSources.class)
public abstract class DamageSourcesMixin {

    @Shadow protected abstract DamageSource source(ResourceKey<DamageType> p_270142_, @Nullable Entity p_270696_);

    @Inject(method = "mobAttack", at = @At("RETURN"), cancellable = true)
    public void onMobAttack(LivingEntity mob, CallbackInfoReturnable<DamageSource> cir)
    {
        if (!ModList.get().isLoaded("bettermobcombat"))
            return;

        EntityPlayer_BetterCombat attacker = (EntityPlayer_BetterCombat) mob;
        AttackHand attackHand = attacker.getCurrentAttack();
        if (attackHand != null)
        {
            DamageClass[] overrides = RPGDamageOverhaulAPI.getBetterCombatAttackOverrides(ForgeRegistries.ITEMS.getKey(attackHand.itemStack().getItem()));
            if (overrides != null)
            {
                int combo = attackHand.combo().current()-1;
                if (combo < overrides.length)
                    cir.setReturnValue(source(overrides[combo].damageTypeKey, mob));
            }
        }
    }


}
