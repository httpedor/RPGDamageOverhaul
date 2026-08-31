package com.httpedor.rpgdamageoverhaul.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.httpedor.rpgdamageoverhaul.damageproperties.HasTagsProperty;
import com.httpedor.rpgdamageoverhaul.damageproperties.IsDamageTypeProperty;

@Mixin(Holder.Reference.class)
public abstract class HolderMixin<T> implements Holder<T> {

    @Shadow public abstract ResourceKey<T> key();

    @Inject(method = "is(Lnet/minecraft/resources/ResourceKey;)Z", at = @At("RETURN"), cancellable = true)
    public void dtAliases(ResourceKey<T> p_205774_, CallbackInfoReturnable<Boolean> cir)
    {
        if (key().isFor(Registries.DAMAGE_TYPE) && !cir.getReturnValue())
        {
            if (!p_205774_.isFor(Registries.DAMAGE_TYPE))
                return;

            if (IsDamageTypeProperty.getDamageTypes(key().location()).contains(p_205774_.location()))
                cir.setReturnValue(true);
        }
    }

    @Inject(method = "is(Lnet/minecraft/resources/ResourceLocation;)Z", at = @At("RETURN"), cancellable = true)
    public void dtAliasesRL(ResourceLocation p_205779_, CallbackInfoReturnable<Boolean> cir)
    {
        if (key().isFor(Registries.DAMAGE_TYPE) && !cir.getReturnValue())
        {
            if (IsDamageTypeProperty.getDamageTypes(key().location()).contains(p_205779_))
                cir.setReturnValue(true);
        }
    }

    @Inject(method = "is(Lnet/minecraft/tags/TagKey;)Z", at = @At("RETURN"), cancellable = true)
    public void tagAliases(TagKey<T> p_205760_, CallbackInfoReturnable<Boolean> cir)
    {
        if (key().isFor(Registries.DAMAGE_TYPE) && !cir.getReturnValue())
        {
            if (!p_205760_.isFor(Registries.DAMAGE_TYPE))
                return;

            if (HasTagsProperty.getTags(key().location()).contains(p_205760_.location()))
                cir.setReturnValue(true);
        }
    }
}
