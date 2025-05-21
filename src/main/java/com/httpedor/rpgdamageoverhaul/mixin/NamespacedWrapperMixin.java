package com.httpedor.rpgdamageoverhaul.mixin;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.ILockableRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraftforge.registries.NamespacedWrapper")
public abstract class NamespacedWrapperMixin<T> extends MappedRegistry<T> implements ILockableRegistry {
    public NamespacedWrapperMixin(ResourceKey<? extends Registry<T>> p_249899_, Lifecycle p_252249_) {
        super(p_249899_, p_252249_);
    }

    @Shadow
    @Final
    private ForgeRegistry<T> delegate;

    @Shadow private boolean frozen;


}
