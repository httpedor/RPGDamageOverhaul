package com.httpedor.rpgdamageoverhaul.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.httpedor.rpgdamageoverhaul.RPGDamageOverhaulFabric;

import net.minecraft.core.RegistryAccess;
import net.minecraft.tags.TagManager;

@Mixin(TagManager.class)
public class TagManagerLoaderMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(RegistryAccess drm, CallbackInfo ci) {
        RPGDamageOverhaulFabric.ra = drm;
    }

}
