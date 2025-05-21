package com.httpedor.rpgdamageoverhaul.mixin;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MappedRegistry.class)
public abstract class SimpleRegistryMixin<T> implements WritableRegistry<T> {

    @Shadow @Final
    ResourceKey<? extends Registry<T>> key;

    @Shadow private boolean frozen;

    @Inject(method = "freeze", at = @At("RETURN"))
    private void freeze(CallbackInfoReturnable<T> cir) {
        if (key.equals(Registries.ATTRIBUTE) || key.equals(Registries.DAMAGE_TYPE))
            this.frozen = false;
    }

}
