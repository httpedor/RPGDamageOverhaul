package com.httpedor.rpgdamageoverhaul.reload;

import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import com.httpedor.rpgdamageoverhaul.Constants;
import com.httpedor.rpgdamageoverhaul.RPGDOLoader;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public class OverridesReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer()).create();
    private RegistryAccess ra;

	public OverridesReloadListener(RegistryAccess ra) {
		super(GSON, "rpgdamageoverhaul");
        this.ra = ra;
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> jsonFiles, ResourceManager resourceManager, ProfilerFiller profiler) {
        for (var entry : jsonFiles.entrySet())
        {
            var location = entry.getKey();
            var element = entry.getValue();
            try
            {
                switch (location.getPath())
                {
                    case "bettercombat":
                        RPGDOLoader.processBetterCombatOverrides(element.getAsJsonObject());
                        break;
                    case "damage_overrides":
                        RPGDOLoader.processDamageOverrides(element.getAsJsonObject());
                        break;
                    case "item_overrides":
                        RPGDOLoader.processItemOverrides(element.getAsJsonObject());
                        break;
                    case "entity_overrides":
                        RPGDOLoader.processEntityOverrides(element.getAsJsonObject());
                        break;
                }
            } catch (Exception e)
            {
                Constants.LOG.error("Error loading datapack file: {}", location, e);
            }
        }
	}
}
