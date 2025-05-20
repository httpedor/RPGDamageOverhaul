package com.httpedor.rpgdamageoverhaul.events;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface DamageClassRegisteredCallback {
    Event<DamageClassRegisteredCallback> EVENT = EventFactory.createArrayBacked(DamageClassRegisteredCallback.class,
            (listeners) -> (damageClass) -> {
                for (DamageClassRegisteredCallback listener : listeners) {
                    listener.interact(damageClass);
                }
            });


    void interact(DamageClass damageClass);
}
