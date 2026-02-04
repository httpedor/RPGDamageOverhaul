package com.httpedor.rpgdamageoverhaul.networking;

import com.httpedor.rpgdamageoverhaul.DatapackLoader;
import com.httpedor.rpgdamageoverhaul.RPGDamageOverhaul;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ConfigurationTask;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public record DamageSyncConfigTask(ServerConfigurationPacketListener listener) implements ICustomConfigurationTask {
    public static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type(ResourceLocation.fromNamespaceAndPath(RPGDamageOverhaul.MODID, "syncDmgs"));
    @Override
    public void run(Consumer<CustomPacketPayload> consumer) {
        consumer.accept(new DamageLoginSyncPacket(DatapackLoader.INSTANCE.dcEntries));
        listener.finishCurrentTask(type());
    }

    @Override
    public @NotNull Type type() {
        return TYPE;
    }
}
