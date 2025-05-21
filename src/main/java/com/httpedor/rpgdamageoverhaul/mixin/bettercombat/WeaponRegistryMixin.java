package com.httpedor.rpgdamageoverhaul.mixin.bettercombat;

import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import net.bettercombat.api.AttributesContainer;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(value = WeaponRegistry.class, remap = false)
public class WeaponRegistryMixin {
    @Shadow
    static Map<ResourceLocation, AttributesContainer> containers;

    @Inject(method = "resolveAttributes", at = @At(value = "INVOKE", target = "Lnet/bettercombat/api/WeaponAttributesHelper;validate(Lnet/bettercombat/api/WeaponAttributes;)V"))
    private static void onAttributeResolve(ResourceLocation itemId, AttributesContainer container, CallbackInfoReturnable<WeaponAttributes> cir)
    {
        if (RPGDamageOverhaulAPI.getBetterCombatAttackOverrides(itemId) != null)
            return;

        var current = itemId;
        while (current != null)
        {
            var currentContainer = containers.get(current);
            if (currentContainer == null)
                break;

            var attackOverrides = RPGDamageOverhaulAPI.getBetterCombatAttackOverrides(current);
            if (attackOverrides != null)
            {
                RPGDamageOverhaulAPI.registerBetterCombatAttackOverrides(itemId, attackOverrides);
                break;
            }

            if (currentContainer.parent() != null)
                current = new ResourceLocation(currentContainer.parent());
            else
                current = null;
        }
    }
}
