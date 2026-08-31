package com.httpedor.rpgdamageoverhaul.damageproperties.onhit;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.OnHitProperty;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public class ChainDamageOnHit extends OnHitProperty {
    private Function<Float, Integer> maxTargetFunction;
    private BiFunction<Float, Integer, Float> damageModifier;
    private Function<Float, Float> rangeFunction;

    public ChainDamageOnHit(Function<Float, Integer> maxTargets, BiFunction<Float, Integer, Float> damageModifier, Function<Float, Float> rangeFunction) {
        this.maxTargetFunction = maxTargets;
        this.damageModifier = damageModifier;
        this.rangeFunction = rangeFunction;
    }

	@Override
	public void onHit(DamageClass dc, LivingEntity target, DamageSource source, double damage) {
	    float range = rangeFunction.apply((float) damage);
		int maxTargets = maxTargetFunction.apply((float) damage);

		Set<LivingEntity> traveled = new HashSet<>();
		LivingEntity current = target;
		while (current != null && traveled.size() < maxTargets)
		{
            traveled.add(current);
            var entities = current.level().getEntitiesOfClass(LivingEntity.class, AABB.ofSize(current.position(), range, range, range), (e) -> !traveled.contains(e));
            if (entities.size() == 0)
                break;
            current = entities.getFirst();
            if (current == null)
                break;
            DamageSource newDmgSource = dc.createDamageSource(source, false);
            var newDamage = damageModifier.apply((float)damage, traveled.size());
            current.hurt(newDmgSource, newDamage);
            var particles = dc.getPropertyOfType(ParticlesOnHit.class);
            if (particles != null)
                particles.onHit(dc, target, newDmgSource, newDamage);
		}
	}
}
