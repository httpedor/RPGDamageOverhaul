package com.httpedor.rpgdamageoverhaul.damageproperties.onhit;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.OnHitProperty;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class FireOnHit extends OnHitProperty {
    float multiplier;
    public FireOnHit(float multiplier)
    {
        this.multiplier = multiplier;
    }

	@Override
	public void onHit(DamageClass dc, LivingEntity target, DamageSource source, double damage) {
	    if (source.getEntity() != null)
            target.igniteForSeconds((float)damage * multiplier);
        target.setTicksFrozen(0);
	}

}
