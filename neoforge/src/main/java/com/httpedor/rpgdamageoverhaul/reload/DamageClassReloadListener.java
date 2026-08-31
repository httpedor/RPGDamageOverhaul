package com.httpedor.rpgdamageoverhaul.reload;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.httpedor.rpgdamageoverhaul.Constants;
import com.httpedor.rpgdamageoverhaul.RPGDOLoader;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class DamageClassReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer()).create();
    private RegistryAccess ra;

    public DamageClassReloadListener(RegistryAccess ra) {
        super(GSON, "rpgdamageoverhaul/damage_class");
        this.ra = ra;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsonFiles, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        RPGDOLoader.clear();
        RPGDamageOverhaulAPI.unloadEverything();

        for (var entry : jsonFiles.entrySet())
        {
            var location = entry.getKey();
            var element = entry.getValue();
            try
            {
                String dcName = location.getPath();
                RPGDOLoader.addDamageClassConfig(dcName, element.getAsJsonObject());
            } catch (Exception e)
            {
                Constants.LOG.error("Error loading damage class file: {}", location, e);
            }
        }

        RPGDOLoader.buildDamageClasses(ra);
    }
}
