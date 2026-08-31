package com.httpedor.rpgdamageoverhaul.damageproperties.onhit;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.OnHitProperty;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class FreezeOnHit extends OnHitProperty {
    public final float multiplier;

    public FreezeOnHit(float multiplier)
    {
        this.multiplier = multiplier;
    }

	@Override
	public void onHit(DamageClass dc, LivingEntity target, DamageSource source, double damage) {
    	target.setTicksFrozen((int) Math.round(damage*multiplier * 20));
        target.setRemainingFireTicks(0);
	}

}
