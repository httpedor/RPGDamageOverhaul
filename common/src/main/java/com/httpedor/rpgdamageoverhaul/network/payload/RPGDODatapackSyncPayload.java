package com.httpedor.rpgdamageoverhaul.network.payload;

import com.httpedor.rpgdamageoverhaul.Constants;
import com.httpedor.rpgdamageoverhaul.RPGDOLoader;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class RPGDODatapackSyncPayload implements CustomPacketPayload {
    public static final Type<RPGDODatapackSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "rpgdo_datapack_sync"));
    public static final RPGDODatapackSyncPayload INSTANCE = new RPGDODatapackSyncPayload();
    public static final StreamCodec<FriendlyByteBuf, RPGDODatapackSyncPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private RPGDODatapackSyncPayload()
    {

    }

    public void handle(RegistryAccess ra)
    {
        RPGDOLoader.buildDamageClasses(ra);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
