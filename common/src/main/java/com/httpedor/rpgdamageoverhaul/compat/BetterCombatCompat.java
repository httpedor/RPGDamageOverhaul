package com.httpedor.rpgdamageoverhaul.compat;

import net.bettercombat.api.AttackHand;
import net.bettercombat.api.EntityPlayer_BetterCombat;
import net.minecraft.world.entity.Entity;

public class BetterCombatCompat {

    public static boolean shouldBCHandleAttack(Entity p)
    {
        AttackHand attackHand = ((EntityPlayer_BetterCombat) p).getCurrentAttack();
        return attackHand != null && !attackHand.itemStack().isEmpty();
    }
}
