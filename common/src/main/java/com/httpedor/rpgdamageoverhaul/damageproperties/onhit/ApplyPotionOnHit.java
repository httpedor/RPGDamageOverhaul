package com.httpedor.rpgdamageoverhaul.damageproperties.onhit;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.DamageClassProperty;
import com.httpedor.rpgdamageoverhaul.api.OnHitProperty;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import oshi.util.tuples.Pair;

public class ApplyPotionOnHit extends OnHitProperty {
    Map<Holder<MobEffect>, Pair<Function<Float, Float>, Function<Float, Float>>> effects = new HashMap<>();

    public ApplyPotionOnHit(Map<Holder<MobEffect>, Pair<Function<Float, Float>, Function<Float, Float>>> effects)
    {
        this.effects = effects;
    }

    @Override
    public void onHit(DamageClass dc, LivingEntity target, DamageSource source, double damage) {
        for (var entry : effects.entrySet())
        {
            var effect = entry.getKey();
            var durationFunc = entry.getValue().getA();
            var amplifierFunc = entry.getValue().getB();
            int duration = (int) (durationFunc.apply((float) damage) * 20);
            int amplifier = amplifierFunc.apply((float) damage).intValue();
            target.addEffect(new MobEffectInstance(effect, duration, amplifier));
        }
    }

    @Override
    public DamageClassProperty mergeWith(DamageClassProperty other) {
        if (other instanceof ApplyPotionOnHit otherProp)
        {
            Map<Holder<MobEffect>, Pair<Function<Float, Float>, Function<Float, Float>>> newEffects = new HashMap<>(otherProp.effects);
            newEffects.putAll(effects);
            return new ApplyPotionOnHit(newEffects);
        }
        return this;
    }
}
