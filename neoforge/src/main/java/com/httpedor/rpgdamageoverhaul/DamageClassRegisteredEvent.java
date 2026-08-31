package com.httpedor.rpgdamageoverhaul;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;

import net.neoforged.bus.api.Event;

public class DamageClassRegisteredEvent extends Event {
    DamageClass dc;

    public DamageClassRegisteredEvent(DamageClass dc)
    {
        this.dc = dc;
    }

    public DamageClass getDamageClass()
    {
        return dc;
    }
}
