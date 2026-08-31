package com.httpedor.rpgdamageoverhaul.network;

import com.httpedor.rpgdamageoverhaul.network.payload.DamageClassAddConfigPayload;
import com.httpedor.rpgdamageoverhaul.network.payload.DamageClassSyncFinishedPayload;
import com.httpedor.rpgdamageoverhaul.network.payload.DamageClassSyncStartPayload;
import com.httpedor.rpgdamageoverhaul.network.payload.RPGDODatapackSyncPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public class ForgeNetworkInit {
    public static void register(RegisterPayloadHandlersEvent e)
    {
        final var registrar = e.registrar("1.0");

        registrar.configurationToClient(DamageClassSyncStartPayload.TYPE, DamageClassSyncStartPayload.STREAM_CODEC, (payload, ctx) -> {
            if (!ctx.connection().isMemoryConnection())
                payload.handle();
        });
        registrar.configurationToClient(DamageClassAddConfigPayload.TYPE, DamageClassAddConfigPayload.STREAM_CODEC, (payload, ctx) -> {
            if (!ctx.connection().isMemoryConnection())
                payload.handle();
        });
        registrar.configurationBidirectional(DamageClassSyncFinishedPayload.TYPE, DamageClassSyncFinishedPayload.STREAM_CODEC, (payload, ctx) -> {
            if (!ctx.connection().isMemoryConnection())
                payload.handle(ctx.flow().isClientbound());
            if (ctx.flow().isServerbound())
                ctx.finishCurrentTask(SyncDamageClassConfiguration.TYPE);
            else
                ctx.reply(DamageClassSyncFinishedPayload.INSTANCE);
        });

        registrar.commonToClient(RPGDODatapackSyncPayload.TYPE, RPGDODatapackSyncPayload.STREAM_CODEC, (payload, ctx) -> {
            if (!ctx.connection().isMemoryConnection())
                payload.handle(ctx.player().registryAccess());
        });

    }
}
