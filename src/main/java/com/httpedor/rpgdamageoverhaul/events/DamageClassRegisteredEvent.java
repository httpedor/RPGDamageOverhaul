package com.httpedor.rpgdamageoverhaul.events;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import net.minecraftforge.eventbus.api.Event;

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
