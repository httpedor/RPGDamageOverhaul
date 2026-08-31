package com.httpedor.rpgdamageoverhaul;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public class RPGDamageOverhaulFabric implements ModInitializer {
    public static RegistryAccess ra = null;

    @Override
    public void onInitialize() {
        CommonClass.init();

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(ResourceLocation.fromNamespaceAndPath("rpgdamageoverhaul", "reload_listener"), (lookup) -> {
            return new SimpleSynchronousResourceReloadListener()
            {
                @Override
                public ResourceLocation getFabricId() {
                    return ResourceLocation.fromNamespaceAndPath("rpgdamageoverhaul", "reload_listener");
                }

                @Override
                public void onResourceManagerReload(ResourceManager manager) {
                    RPGDamageOverhaulAPI.unloadEverything();
                    //Read damage classes
                    for (Map.Entry<ResourceLocation, Resource> entry : manager.listResources("rpgdamageoverhaul", path -> path.getPath().equals("rpgdamageoverhaul/damage_classes.json")).entrySet())
                    {
                        try (InputStream stream = manager.getResource(entry.getKey()).get().open()) {
                            InputStreamReader reader = new InputStreamReader(stream);
                            JsonObject obj = (JsonObject) JsonParser.parseReader(reader);
                            for (Map.Entry<String, JsonElement> dcJson : obj.entrySet())
                                RPGDOLoader.registerDamageClass(dcJson.getKey(), dcJson.getValue().getAsJsonObject(), null, ra);
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }

                    //Read damage overrides
                    for (Map.Entry<ResourceLocation, Resource> entry : manager.listResources("rpgdamageoverhaul", path -> path.getPath().equals("rpgdamageoverhaul/damage_overrides.json")).entrySet())
                    {
                        try (InputStream stream = manager.getResource(entry.getKey()).get().open()) {
                            InputStreamReader reader = new InputStreamReader(stream);
                            JsonObject obj = (JsonObject) JsonParser.parseReader(reader);
                            RPGDOLoader.processDamageOverrides(obj);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    //Read better combat damage types
                    for (Map.Entry<ResourceLocation, Resource> entry : manager.listResources("rpgdamageoverhaul", path -> path.getPath().equals("rpgdamageoverhaul/bettercombat.json")).entrySet())
                    {
                        try (InputStream stream = manager.getResource(entry.getKey()).get().open()) {
                            InputStreamReader reader = new InputStreamReader(stream);
                            JsonObject obj = (JsonObject) JsonParser.parseReader(reader);
                            RPGDOLoader.processBetterCombatOverrides(obj);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    //Read item overrides
                    for (Map.Entry<ResourceLocation, Resource> entry : manager.listResources("rpgdamageoverhaul", path -> path.getPath().equals("rpgdamageoverhaul/item_overrides.json")).entrySet())
                    {
                        try (InputStream stream = manager.getResource(entry.getKey()).get().open()) {
                            InputStreamReader reader = new InputStreamReader(stream);
                            JsonObject obj = (JsonObject) JsonParser.parseReader(reader);
                            RPGDOLoader.processItemOverrides(obj);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    for (Map.Entry<ResourceLocation, Resource> entry : manager.listResources("rpgdamageoverhaul", path -> path.getPath().equals("rpgdamageoverhaul/entity_overrides.json")).entrySet())
                    {

                        try (InputStream stream = manager.getResource(entry.getKey()).get().open()) {
                            InputStreamReader reader = new InputStreamReader(stream);
                            JsonObject obj = (JsonObject) JsonParser.parseReader(reader);
                            RPGDOLoader.processEntityOverrides(obj);
                        } catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                }

            };
        });
    }
}
