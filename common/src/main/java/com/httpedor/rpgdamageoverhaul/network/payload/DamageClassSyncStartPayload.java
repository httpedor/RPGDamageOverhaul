package com.httpedor.rpgdamageoverhaul.network.payload;


import com.httpedor.rpgdamageoverhaul.Constants;
import com.httpedor.rpgdamageoverhaul.RPGDOLoader;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @param attributes every attribute id in the server's ATTRIBUTE registry that belongs to us. Deliberately taken
 *                   from the registry and not from the damage class list -- see
 *                   {@link RPGDamageOverhaulAPI#registerSyncedAttributes}.
 */
public record DamageClassSyncStartPayload(List<ResourceLocation> attributes) implements CustomPacketPayload {
    public static final Type<DamageClassSyncStartPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "damage_class_sync_started"));
    public static final StreamCodec<FriendlyByteBuf, DamageClassSyncStartPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeCollection(payload.attributes, FriendlyByteBuf::writeResourceLocation),
            buf -> new DamageClassSyncStartPayload(buf.readList(FriendlyByteBuf::readResourceLocation)));

    public void handle()
    {
        RPGDamageOverhaulAPI.unloadEverything();
        RPGDOLoader.clear();
        // Register the server's attributes before NeoForge's registry sync task runs (SyncBeforeForgeMixin puts our
        // configuration task ahead of it). Ghost attributes get no damage class and stay out of the entity
        // suppliers; they only need to exist so the snapshot the server sends doesn't name anything we lack.
        RPGDamageOverhaulAPI.registerSyncedAttributes(attributes);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
