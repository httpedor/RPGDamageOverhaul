package com.httpedor.rpgdamageoverhaul.damageproperties.onhit;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.OnHitProperty;
import com.httpedor.rpgdamageoverhaul.ducktypes.DamageClassEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Function;

public class SetWetOnHit extends OnHitProperty {
    Function<Float, Float> durationFunc;

    public SetWetOnHit(Function<Float, Float> durationFunc) {
        this.durationFunc = durationFunc;
    }
    public SetWetOnHit(float duration) {
        this.durationFunc = (dmg) -> duration;
    }

    @Override
    public void onHit(DamageClass dc, LivingEntity target, DamageSource source, double damage) {
        DamageClassEntityData.of(target).rpgdo$setWetUntil(target.level().getGameTime() + (long)(durationFunc.apply((float)damage) * 20));
    }

    public static boolean isWet(LivingEntity entity) {
        return entity.isInWaterRainOrBubble() || DamageClassEntityData.of(entity).rpgdo$getWetUntil() > entity.level().getGameTime();
    }
}
