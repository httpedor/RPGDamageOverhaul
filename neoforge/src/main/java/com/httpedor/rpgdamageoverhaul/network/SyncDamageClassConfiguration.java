package com.httpedor.rpgdamageoverhaul.network;

import com.httpedor.rpgdamageoverhaul.Constants;
import com.httpedor.rpgdamageoverhaul.RPGDOLoader;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.httpedor.rpgdamageoverhaul.network.payload.DamageClassAddConfigPayload;
import com.httpedor.rpgdamageoverhaul.network.payload.DamageClassSyncFinishedPayload;
import com.httpedor.rpgdamageoverhaul.network.payload.DamageClassSyncStartPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class SyncDamageClassConfiguration implements ICustomConfigurationTask {
	private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "sync_damage_classes");
	public static final Type TYPE = new Type(ID);



	@Override
	public @NotNull Type type() {
	    return TYPE;
	}

	@Override
	public void run(Consumer<CustomPacketPayload> consumer) {
		// Taken from the registry rather than from the damage classes: attributes of a class a /reload removed
		// linger there and are still in the snapshot NeoForge syncs, and a client that lacks any of them is kicked.
		List<ResourceLocation> attributeIds = BuiltInRegistries.ATTRIBUTE.keySet().stream()
				.filter(id -> id.getNamespace().equals(Constants.MOD_ID))
				.sorted(Comparator.comparing(ResourceLocation::toString))
				.toList();
		consumer.accept(new DamageClassSyncStartPayload(attributeIds));
		for (var dc : RPGDamageOverhaulAPI.getAllDamageClasses())
		{
			String name = dc.name;
			var jsonData = RPGDOLoader.getDamageClassOriginalJsons(name);
			for (var json : jsonData)
			{
				consumer.accept(new DamageClassAddConfigPayload(name, json));
			}
		}
		consumer.accept(DamageClassSyncFinishedPayload.INSTANCE);
	}
}
