package com.httpedor.rpgdamageoverhaul.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.httpedor.rpgdamageoverhaul.RPGDamageOverhaul;
import com.httpedor.rpgdamageoverhaul.ReloadListener;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.tag.TagManagerLoader;

@Mixin(TagManagerLoader.class)
public class TagManagerLoaderMixin {
    
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(DynamicRegistryManager drm, CallbackInfo ci) {
        ReloadListener.ra = drm;
    }

}
