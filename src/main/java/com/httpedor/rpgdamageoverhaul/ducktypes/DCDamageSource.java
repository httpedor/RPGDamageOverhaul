package com.httpedor.rpgdamageoverhaul.ducktypes;

public interface DCDamageSource {

    boolean shouldTriggerOnHitEffects();
    void setTriggerOnHitEffects(boolean trigger);

}
