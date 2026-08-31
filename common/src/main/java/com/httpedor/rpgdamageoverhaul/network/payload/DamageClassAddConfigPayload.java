package com.httpedor.rpgdamageoverhaul.network.payload;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;import com.google.gson.JsonParser;
import com.httpedor.rpgdamageoverhaul.Constants;
import com.httpedor.rpgdamageoverhaul.RPGDOLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// Yeah, this is ugly and lazy. Yeah, I should make StreamCodecs for each property type and send the already merged properties.
// You got some free time? Send a PR!
public record DamageClassAddConfigPayload(String name, JsonObject json) implements CustomPacketPayload {
    public static final Type<DamageClassAddConfigPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "damage_class_add"));
    public static final StreamCodec<FriendlyByteBuf, DamageClassAddConfigPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUtf(payload.name);
                buf.writeUtf(payload.json.toString());
            },
            buf -> {
                String name = buf.readUtf();
                String jsonStr = buf.readUtf();
                JsonObject properties = JsonParser.parseString(jsonStr).getAsJsonObject();
                return new DamageClassAddConfigPayload(name, properties);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle() {
        RPGDOLoader.addDamageClassConfig(name, json);
    }
}
