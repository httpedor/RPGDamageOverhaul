package com.httpedor.rpgdamageoverhaul.compat;

import dev.shadowsoffire.attributeslib.api.ALCombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class ApothicAttributesCompat {

    public static float applyAAArmor(LivingEntity target, DamageSource ds, float amount, float armor, float toughness)
    {
        return ALCombatRules.getDamageAfterArmor(target ,ds, amount, armor, toughness);
    }

}
