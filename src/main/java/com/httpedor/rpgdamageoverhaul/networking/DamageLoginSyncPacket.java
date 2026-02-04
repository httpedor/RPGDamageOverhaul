package com.httpedor.rpgdamageoverhaul.networking;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.httpedor.rpgdamageoverhaul.DatapackLoader;
import com.httpedor.rpgdamageoverhaul.RPGDamageOverhaul;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public record DamageLoginSyncPacket(Map<String, JsonObject> entries) implements CustomPacketPayload {

    public static final Type<DamageLoginSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RPGDamageOverhaul.MODID, "rpgdologinpacket"));

    public static final StreamCodec<ByteBuf, DamageLoginSyncPacket> DECODER = StreamCodec.ofMember(
            DamageLoginSyncPacket::write,
            DamageLoginSyncPacket::read
    );

    public static final IPayloadHandler<DamageLoginSyncPacket> HANDLER = (packet, ctx) -> {
        ctx.enqueueWork(() -> {
            for (Map.Entry<String, JsonObject> entry : packet.entries.entrySet()) {
                DatapackLoader.INSTANCE.registerDamageClass(entry.getKey(), entry.getValue(), null);
            }
        });
    };

    public static DamageLoginSyncPacket read(ByteBuf bbuf) {
        HashMap<String, JsonObject> map = new HashMap<>();
        var buf = new FriendlyByteBuf(bbuf);
        buf.readMap(i -> map, FriendlyByteBuf::readUtf, (res) -> JsonParser.parseString(res.readUtf()).getAsJsonObject());
        return new DamageLoginSyncPacket(map);
    }

    public void write(ByteBuf bbuf) {
        var buf = new FriendlyByteBuf(bbuf);
        buf.writeMap(entries, FriendlyByteBuf::writeUtf, (b, json) -> b.writeUtf(json.toString()));
    }


    @Override
    public Type<DamageLoginSyncPacket> type() {
        return TYPE;
    }
}
