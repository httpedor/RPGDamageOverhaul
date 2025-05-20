package com.httpedor.rpgdamageoverhaul.mixin;

import com.httpedor.rpgdamageoverhaul.RPGDamageOverhaul;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RegistryEntry.Reference.class)
public abstract class ReferenceMixin<T> implements RegistryEntry<T> {
    @Shadow
    public abstract RegistryKey<T> registryKey();

    @Inject(method = "matchesKey", at = @At("RETURN"), cancellable = true)
    public void dtAliases(RegistryKey<T> key, CallbackInfoReturnable<Boolean> cir)
    {
        if (registryKey().isOf(RegistryKeys.DAMAGE_TYPE) && !cir.getReturnValue())
        {
            if (!key.isOf(RegistryKeys.DAMAGE_TYPE))
                return;

            if (!RPGDamageOverhaul.mappedDamageTypes.containsKey(registryKey().getValue()))
                return;

            for (var mapped : RPGDamageOverhaul.mappedDamageTypes.get(registryKey().getValue()))
            {
                if (mapped.equals(key.getValue()))
                {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }

    @Inject(method = "matchesId", at = @At("RETURN"), cancellable = true)
    public void dtAliasesRL(Identifier id, CallbackInfoReturnable<Boolean> cir)
    {
        if (registryKey().isOf(RegistryKeys.DAMAGE_TYPE) && !cir.getReturnValue())
        {
            if (!RPGDamageOverhaul.mappedDamageTypes.containsKey(registryKey().getValue()))
                return;

            for (var mapped : RPGDamageOverhaul.mappedDamageTypes.get(registryKey().getValue()))
            {
                if (mapped.equals(id))
                {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }

    @Inject(method = "isIn", at = @At("RETURN"), cancellable = true)
    public void tagAliases(TagKey<T> p_205760_, CallbackInfoReturnable<Boolean> cir)
    {
        if (registryKey().isOf(RegistryKeys.DAMAGE_TYPE) && !cir.getReturnValue())
        {
            if (!p_205760_.isOf(RegistryKeys.DAMAGE_TYPE))
                return;

            if (!RPGDamageOverhaul.mappedTags.containsKey(registryKey().getValue()))
                return;

            for (var mapped : RPGDamageOverhaul.mappedTags.get(registryKey().getValue()))
            {
                if (mapped.equals(p_205760_.id()))
                {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }
}
