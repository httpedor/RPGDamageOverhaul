package com.httpedor.rpgdamageoverhaul.mixin;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.fabric.impl.resource.loader.ResourceManagerHelperImpl;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ResourceManagerHelperImpl.class)
public class RMHIMixin {

    @Inject(at = @At("RETURN"), method = "sort(Ljava/util/List;)V", remap = false)
    private void loadRPGDOFirst(List<ResourceReloader> listeners, CallbackInfo ci)
    {
        SimpleSynchronousResourceReloadListener RPGDOListener = null;

        for (int i = 0; i < listeners.size(); i++)
        {
            if (listeners.get(i) instanceof SimpleSynchronousResourceReloadListener ssrrl && ssrrl.getFabricId().equals(new Identifier("rpgdamageoverhaul", "read_damage_types")))
            {
                RPGDOListener = ssrrl;
                listeners.remove(i);
                break;
            }
        }

        if (RPGDOListener != null)
            listeners.add(0, RPGDOListener);
    }

}
