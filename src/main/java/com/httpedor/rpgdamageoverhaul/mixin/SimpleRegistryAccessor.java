package com.httpedor.rpgdamageoverhaul.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.registry.SimpleRegistry;

@Mixin(SimpleRegistry.class)
public interface SimpleRegistryAccessor {
    @Accessor("frozen")
    boolean getFrozen();

    @Accessor("frozen")
    void setFrozen(boolean frozen);
} 