package com.httpedor.rpgdamageoverhaul.network.payload;

import com.httpedor.rpgdamageoverhaul.Constants;
import com.httpedor.rpgdamageoverhaul.RPGDOLoader;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DamageClassSyncFinishedPayload implements CustomPacketPayload {
    public static final Type<DamageClassSyncFinishedPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "damage_class_sync_finished"));
    public static final DamageClassSyncFinishedPayload INSTANCE = new DamageClassSyncFinishedPayload();
    public static final StreamCodec<FriendlyByteBuf, DamageClassSyncFinishedPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private DamageClassSyncFinishedPayload() {}

    @Override
    public @NotNull Type<DamageClassSyncFinishedPayload> type() {
        return TYPE;
    }

    public void handle(boolean isClient)
    {
        if (isClient)
        {
            // Register attributes right now, but the properties will be stored in RPGDOLoader until RPGDODatapackSyncPayload
            // is received, at which point the properties will be merged and the actual DamageClasses will be built.
            // We send this right now so that the client doesn't disconnect when the server tries to sync the attribute
            // registry, which happens before RPGDODatapackSyncPayload is sent.
            List<Holder<Attribute>> attributes = new ArrayList<>();
            for (var name : RPGDOLoader.getDamageClassNames())
                attributes.addAll(RPGDamageOverhaulAPI.registerDamageClassAttributes(name).values());

            // The entity attribute suppliers have to be patched here too, and not just in registerDamageClass: this
            // runs during the configuration phase, while RPGDODatapackSyncPayload only arrives once we're already in
            // play, after the LocalPlayer has been built from the login packet. A LocalPlayer built from a supplier
            // that predates the injection is missing every damage class attribute for the rest of its life, and the
            // client-side half of Player.attack reads those attributes on the first swing.
            RPGDamageOverhaulAPI.injectAttributesIntoEntityTypes(attributes);
        }
    }
}
