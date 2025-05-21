package com.httpedor.rpgdamageoverhaul.mixin.bettercombat;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.EntityPlayer_BetterCombat;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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

    @Shadow
    protected abstract DamageSource source(ResourceKey<DamageType> key, @Nullable Entity attacker);

    @Inject(method = "playerAttack", at = @At("RETURN"), cancellable = true)
    public void onPlayerAttack(Player attacker, CallbackInfoReturnable<DamageSource> cir) {
        if (!ModList.get().isLoaded("bettercombat"))
            return;
        AttackHand attackHand = ((EntityPlayer_BetterCombat) attacker).getCurrentAttack();
        if (attackHand != null)
        {
            DamageClass[] attackOverrides = RPGDamageOverhaulAPI.getBetterCombatAttackOverrides(ForgeRegistries.ITEMS.getKey(attackHand.itemStack().getItem()));
            if (attackOverrides != null && attackOverrides.length > 0)
            {

                int combo = attackHand.combo().current()-1;
                if (combo < attackOverrides.length)
                    cir.setReturnValue(source(attackOverrides[combo].damageTypeKey, attacker));
            }
        }
    }

}
