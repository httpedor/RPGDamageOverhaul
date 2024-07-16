package com.httpedor.rpgdamageoverhaul.mixin;

import net.minecraft.registry.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SimpleRegistry.class)
public abstract class SimpleRegistryMixin<T> implements MutableRegistry<T>{

    @Shadow @Final private RegistryKey<? extends Registry<T>> key;

    @Shadow private boolean frozen;

    @Inject(method = "freeze", at = @At("RETURN"))
    private void freeze(CallbackInfoReturnable<T> cir) {
        if (key.equals(RegistryKeys.ATTRIBUTE) || key.equals(RegistryKeys.DAMAGE_TYPE))
            this.frozen = false;
    }

}
