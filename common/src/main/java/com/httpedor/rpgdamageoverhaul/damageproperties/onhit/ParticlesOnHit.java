package com.httpedor.rpgdamageoverhaul.damageproperties.onhit;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.OnHitProperty;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class ParticlesOnHit extends OnHitProperty {
    public final SimpleParticleType particle;
    public ParticlesOnHit(SimpleParticleType particle)
    {
        this.particle = particle;
    }

    @Override
	public void onHit(DamageClass dc, LivingEntity target, DamageSource source, double damage) {
    	double maxSpeed  = 0.1;
        var world = (ServerLevel) target.level();
        var amount = (int) (damage/2)+1;
        for (int i = 0; i < amount; i++)
            world.sendParticles(particle, target.getX(), target.getEyeY(), target.getZ(), amount, 0d, 0d, 0d, maxSpeed);
	}

}
