package com.httpedor.rpgdamageoverhaul.damageproperties.onhit;

import java.util.function.Function;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.OnHitProperty;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class HealOnHit extends OnHitProperty {
    private final Function<Float, Float> healAmount;

    public HealOnHit(Function<Float, Float> healAmount) {
        this.healAmount = healAmount;
    }

    public float getHealAmount(float damage) {
        return healAmount.apply(damage);
    }

	@Override
	public void onHit(DamageClass dc, LivingEntity target, DamageSource source, double damage) {
        target.heal(getHealAmount((float) damage));
	}
}
