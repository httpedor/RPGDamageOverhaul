package com.httpedor.rpgdamageoverhaul.mixin;

import com.httpedor.rpgdamageoverhaul.ducktypes.DCDamageSource;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(DamageSource.class)
public class DamageSourceMixin implements DCDamageSource {
    @Unique
    private boolean trigger = true;

    @Override
    public boolean shouldTriggerOnHitEffects() {
        return trigger;
    }

    @Override
    public void setTriggerOnHitEffects(boolean trigger) {
        this.trigger = trigger;
    }


}
