package com.httpedor.rpgdamageoverhaul.damageproperties.onhit;

import java.util.Set;
import java.util.function.Function;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.OnHitProperty;
import com.httpedor.rpgdamageoverhaul.ducktypes.DamageClassEntityData;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class ModifyDamageOnHit extends OnHitProperty {
    public record DamageModificationInstance(ModifyDamageOnHit config, long expirationTick) {}
    public final Function<Float, Float> damageModifier;
    public final Function<Float, Float> durationFormula;
    public final Set<String> exceptions;
    public final boolean isWhitelist;
    public ModifyDamageOnHit(Function<Float, Float> damageModifier, Function<Float, Float> durationFormula, Set<String> exceptions, boolean isWhitelist) {
        this.damageModifier = damageModifier;
        this.durationFormula = durationFormula;
        this.exceptions = exceptions;
        this.isWhitelist = isWhitelist;
    }

    @Override
    public void onHit(DamageClass dc, LivingEntity target, DamageSource source, double damage) {
        var duration = target.level().getGameTime() + (long)(durationFormula.apply((float)damage) * 20);
        var modInstance = new DamageModificationInstance(this, duration);
        DamageClassEntityData.of(target).rpgdo$getDamageModifications().put(this, modInstance);
    }

    public static float applyDamageModifiers(LivingEntity target, float damage, DamageClass dc) {
        var mods = DamageClassEntityData.of(target).rpgdo$getDamageModifications();
        if (mods.isEmpty()) return damage;
        var it = mods.entrySet().iterator();
        while (it.hasNext())
        {
            var entry = it.next();
            var mod = entry.getKey();
            var instance = entry.getValue();
            if (instance.expirationTick < target.level().getGameTime())
            {
                it.remove();
                continue;
            }
            if (mod.exceptions.contains(dc.name) == mod.isWhitelist)
                damage = mod.damageModifier.apply(damage);
        }
        return damage;
    }
}
