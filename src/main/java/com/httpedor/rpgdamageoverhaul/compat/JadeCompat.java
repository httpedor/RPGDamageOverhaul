package com.httpedor.rpgdamageoverhaul.compat;

import com.httpedor.rpgdamageoverhaul.compat.jade.ResistanceComponentProvider;
import net.minecraft.entity.LivingEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class JadeCompat implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        IWailaPlugin.super.register(registration);
        registration.registerEntityDataProvider(ResistanceComponentProvider.INSTANCE, LivingEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        IWailaPlugin.super.registerClient(registration);

        registration.registerEntityComponent(ResistanceComponentProvider.INSTANCE, LivingEntity.class);
    }
}
