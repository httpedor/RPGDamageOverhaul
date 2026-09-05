package com.httpedor.rpgdamageoverhaul.network.payload;

import java.util.HashMap;
import java.util.Map;

import com.httpedor.rpgdamageoverhaul.ClientAbsorptionData;
import com.httpedor.rpgdamageoverhaul.Constants;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Clientbound: the receiving player's current per-damage-class absorption pools, for the HUD. */
public record AbsorptionSyncPayload(Map<String, Float> pools) implements CustomPacketPayload {
    public static final Type<AbsorptionSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "absorption_sync"));
    public static final StreamCodec<FriendlyByteBuf, AbsorptionSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.pools.size());
                for (var entry : payload.pools.entrySet()) {
                    buf.writeUtf(entry.getKey());
                    buf.writeFloat(entry.getValue());
                }
            },
            buf -> {
                int size = buf.readVarInt();
                Map<String, Float> pools = new HashMap<>(size);
                for (int i = 0; i < size; i++) {
                    String name = buf.readUtf();
                    pools.put(name, buf.readFloat());
                }
                return new AbsorptionSyncPayload(pools);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle() {
        ClientAbsorptionData.set(pools);
    }
}
