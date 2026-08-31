package com.httpedor.rpgdamageoverhaul.damageproperties.onhit;

import java.util.function.BiFunction;
import java.util.function.Function;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.OnHitProperty;
import com.httpedor.rpgdamageoverhaul.ducktypes.DamageClassEntityData;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class HealingModifierOnHit extends OnHitProperty {
    /** A heal modification in effect on one entity. Transient, like the hit that applied it. */
    public record ActiveHealModifier(long expirationTick, double damage) {}

    BiFunction<Float, Float, Float> healModifier;
    Function<Float, Float> duration;
    public HealingModifierOnHit(BiFunction<Float, Float, Float> healModifier, Function<Float, Float> duration) {
        this.healModifier = healModifier;
        this.duration = duration;
    }

    @Override
    public void onHit(DamageClass dc, LivingEntity target, DamageSource source, double damage) {
        long currentTime = target.level().getGameTime();
        float targetDuration = duration.apply((float)damage);
        long targetTime = currentTime + (long)(targetDuration * 20);
        DamageClassEntityData.of(target).rpgdo$getActiveHealModifiers().put(this, new ActiveHealModifier(targetTime, damage));
    }

    public static float modifyHealing(LivingEntity entity, float healing) {
        var active = DamageClassEntityData.of(entity).rpgdo$getActiveHealModifiers();
        if (active.isEmpty())
            return healing;

        var it = active.entrySet().iterator();
        while (it.hasNext())
        {
            var entry = it.next();
            var modifier = entry.getKey();
            var instance = entry.getValue();
            if (entity.level().getGameTime() > instance.expirationTick())
            {
                it.remove();
                continue;
            }
            healing = modifier.healModifier.apply(healing, (float) instance.damage());
        }
        return healing;
    }
}
