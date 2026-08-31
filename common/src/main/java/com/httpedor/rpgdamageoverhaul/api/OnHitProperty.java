package com.httpedor.rpgdamageoverhaul.api;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public abstract class OnHitProperty extends DamageClassProperty
{
    public abstract void onHit(DamageClass dc, LivingEntity target, DamageSource source, double damage);
}
