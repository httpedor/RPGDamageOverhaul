package com.httpedor.rpgdamageoverhaul;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import com.httpedor.rpgdamageoverhaul.api.DamageClass;

public interface DamageClassRegisteredCallback {
    Event<DamageClassRegisteredCallback> EVENT = EventFactory.createArrayBacked(DamageClassRegisteredCallback.class,
            (listeners) -> (damageClass) -> {
                for (DamageClassRegisteredCallback listener : listeners) {
                    listener.interact(damageClass);
                }
            });


    void interact(DamageClass damageClass);
}
