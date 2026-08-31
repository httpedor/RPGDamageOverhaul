package com.httpedor.rpgdamageoverhaul.mixin;

import com.httpedor.rpgdamageoverhaul.network.SyncDamageClassConfiguration;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;

@Mixin(ServerConfigurationPacketListenerImpl.class)
public class SyncBeforeForgeMixin {

    @Shadow
    @Final
    private Queue<ConfigurationTask> configurationTasks;

    @Inject(method = "runConfiguration", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/network/ConfigurationInitialization;configureEarlyTasks(Lnet/minecraft/network/protocol/configuration/ServerConfigurationPacketListener;Ljava/util/function/Consumer;)V"))
    private void runDCSyncPacket(CallbackInfo ci)
    {
        this.configurationTasks.add(new SyncDamageClassConfiguration());
    }

}
