package com.httpedor.rpgdamageoverhaul;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;

import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.DataPackContents;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class ReloadListener implements SimpleSynchronousResourceReloadListener {
    
    public HashMap<String, JsonObject> dcEntries = new HashMap<>();
    public JsonObject damageOverrides = new JsonObject();
    public JsonObject entityOverrides = new JsonObject();
    public JsonObject itemOverrides = new JsonObject();
    public JsonObject betterCombatOverrides = new JsonObject();

    public static DynamicRegistryManager ra;

    @Override
    public Identifier getFabricId() {
        return new Identifier("rpgdamageoverhaul", "read_damage_types");
    }

    public void registerDamageClass(String name, JsonObject obj, DamageClass parent)
    {
        List<Identifier> onHitEffects = new ArrayList<>();
        if (obj.has("onHit"))
            onHitEffects = obj.getAsJsonArray("onHit").asList().stream().map(JsonElement::getAsString).map(Identifier::new).toList();

        String dmgAttribute = obj.has("damage") ? obj.get("damage").getAsString() : "rpgdamageoverhaul:" + name + "." + "damage";
        String armorAttribute = obj.has("armor") ? obj.get("armor").getAsString() : "rpgdamageoverhaul:" + name + "." + "armor";
        String absorptionAttribute = obj.has("absorption") ? obj.get("absorption").getAsString() : "rpgdamageoverhaul:" + name + "." + "absorption";
        String resistanceAttribute = obj.has("resistance") ? obj.get("resistance").getAsString() : "rpgdamageoverhaul:" + name + "." + "resistance";

        DamageClass dc = RPGDamageOverhaulAPI.registerDamage(name, parent == null ? null : parent.name, new RPGDamageOverhaulAPI.DamageClassAttributes(dmgAttribute, armorAttribute, absorptionAttribute, resistanceAttribute), ra);
        dc.properties = obj.asMap();

        for (Identifier effect : onHitEffects)
        {
            dc.addOnHitEffect(effect);
        }

        if (obj.has("subClasses")) {
            for (Map.Entry<String, JsonElement> subClass : obj.getAsJsonObject("subClasses").entrySet()) {
                registerDamageClass(subClass.getKey(), subClass.getValue().getAsJsonObject(), RPGDamageOverhaulAPI.getDamageClass(name));
            }
        }
        dcEntries.put(name, obj);
    }

    void processBetterCombatOverrides(JsonObject obj)
    {
        for (Map.Entry<String, JsonElement> attacksEntry : obj.entrySet())
        {
            betterCombatOverrides.add(attacksEntry.getKey(), attacksEntry.getValue());
            JsonArray arr = attacksEntry.getValue().getAsJsonArray();
            DamageClass[] dcs = new DamageClass[arr.size()];
            for (int i = 0; i < arr.size(); i++)
            {
                dcs[i] = RPGDamageOverhaulAPI.getDamageClass(arr.get(i).getAsString());
                if (dcs[i] == null)
                    RPGDamageOverhaul.LOGGER.warn("Damage class not found: {}", arr.get(i).getAsString());
            }
            RPGDamageOverhaulAPI.registerBetterCombatAttackOverrides(new Identifier(attacksEntry.getKey()), dcs);

            Map<DamageClass, Integer> counts = new HashMap<>();
            for (DamageClass dc : dcs) {
                counts.put(dc, counts.getOrDefault(dc, 0) + 1);
            }

            Map<DamageClass, Double> fallbackItemOverrides = new HashMap<>();
            for (Map.Entry<DamageClass, Integer> count : counts.entrySet())
            {
                fallbackItemOverrides.put(count.getKey(), 1.0 / dcs.length * count.getValue());
            }
            RPGDamageOverhaulAPI.registerItemOverrides(new Identifier(attacksEntry.getKey()), fallbackItemOverrides);
        }
    }
    void processDamageOverrides(JsonObject obj)
    {
        for (Map.Entry<String, JsonElement> entry : obj.entrySet())
        {
            damageOverrides.add(entry.getKey(), entry.getValue());
            Identifier mcDamageType = new Identifier(entry.getKey());
            Map<DamageClass, Double> overrides = new HashMap<>();
            JsonObject overridesObj = entry.getValue().getAsJsonObject();
            for (Map.Entry<String, JsonElement> override : overridesObj.entrySet())
            {
                DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(override.getKey());
                if (dc == null)
                {
                    RPGDamageOverhaul.LOGGER.warn("Unknown damage class: {} for override: {}", override.getKey(), mcDamageType);
                    continue;
                }
                overrides.put(dc, override.getValue().getAsDouble());
            }
            RPGDamageOverhaulAPI.registerOverride(mcDamageType,overrides);
        }
    }
    void processItemOverrides(JsonObject obj)
    {
        for (Map.Entry<String, JsonElement> itemOverride : obj.entrySet()) {
            itemOverrides.add(itemOverride.getKey(), itemOverride.getValue());
            boolean isTag;
            Identifier id;
            if (itemOverride.getKey().startsWith("#"))
            {
                isTag = true;
                id = new Identifier(itemOverride.getKey().substring(1));
            }
            else
            {
                isTag = false;
                id = new Identifier(itemOverride.getKey());
            }
            Map<DamageClass, Double> overrides = new HashMap<>();
            for (Map.Entry<String, JsonElement> overrideEntry : itemOverride.getValue().getAsJsonObject().entrySet()) {
                DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(overrideEntry.getKey());
                if (dc != null)
                    overrides.put(dc, overrideEntry.getValue().getAsDouble());
            }
            if (!isTag)
                RPGDamageOverhaulAPI.registerItemOverrides(id, overrides);
            else
                RPGDamageOverhaulAPI.registerItemTagOverrides(id, overrides);
        }
    }

    void processEntityOverrides(JsonObject obj)
    {
        for (Map.Entry<String, JsonElement> entityOverride : obj.entrySet()) {
            entityOverrides.add(entityOverride.getKey(), entityOverride.getValue());
            boolean isTag = false;
            Identifier id;
            if (entityOverride.getKey().startsWith("#"))
            {
                isTag = true;
                id = new Identifier(entityOverride.getKey().substring(1));
            }
            else
            {
                id = new Identifier(entityOverride.getKey());
            }
            Map<DamageClass, Double> overrides = new HashMap<>();
            for (Map.Entry<String, JsonElement> overrideEntry : entityOverride.getValue().getAsJsonObject().entrySet()) {
                DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(overrideEntry.getKey());
                if (dc != null)
                    overrides.put(dc, overrideEntry.getValue().getAsDouble());
            }
            if (isTag)
                RPGDamageOverhaulAPI.registerEntityTagOverrides(id, overrides);
            else
                RPGDamageOverhaulAPI.registerEntityOverrides(id, overrides);
        }
    }

    @Override
    public void reload(ResourceManager manager) {
        RPGDamageOverhaulAPI.unloadEverything();
        //Read damage classes
        for (Map.Entry<Identifier, Resource> entry : manager.findResources("rpgdamageoverhaul", path -> path.getPath().equals("rpgdamageoverhaul/damage_classes.json")).entrySet())
        {
            try (InputStream stream = manager.getResource(entry.getKey()).get().getInputStream()) {
                InputStreamReader reader = new InputStreamReader(stream);
                JsonObject obj = (JsonObject) JsonParser.parseReader(reader);
                for (Map.Entry<String, JsonElement> dcJson : obj.entrySet())
                {
                    registerDamageClass(dcJson.getKey(), dcJson.getValue().getAsJsonObject(), null);
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        //Read damage overrides
        for (Map.Entry<Identifier, Resource> entry : manager.findResources("rpgdamageoverhaul", path -> path.getPath().equals("rpgdamageoverhaul/damage_overrides.json")).entrySet())
        {
            try (InputStream stream = manager.getResource(entry.getKey()).get().getInputStream()) {
                InputStreamReader reader = new InputStreamReader(stream);
                JsonObject obj = (JsonObject) JsonParser.parseReader(reader);
                processDamageOverrides(obj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //Read better combat damage types
        for (Map.Entry<Identifier, Resource> entry : manager.findResources("rpgdamageoverhaul", path -> path.getPath().equals("rpgdamageoverhaul/bettercombat.json")).entrySet())
        {
            try (InputStream stream = manager.getResource(entry.getKey()).get().getInputStream()) {
                InputStreamReader reader = new InputStreamReader(stream);
                JsonObject obj = (JsonObject) JsonParser.parseReader(reader);
                processBetterCombatOverrides(obj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //Read item overrides
        for (Map.Entry<Identifier, Resource> entry : manager.findResources("rpgdamageoverhaul", path -> path.getPath().equals("rpgdamageoverhaul/item_overrides.json")).entrySet())
        {
            try (InputStream stream = manager.getResource(entry.getKey()).get().getInputStream()) {
                InputStreamReader reader = new InputStreamReader(stream);
                JsonObject obj = (JsonObject) JsonParser.parseReader(reader);
                processItemOverrides(obj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (Map.Entry<Identifier, Resource> entry : manager.findResources("rpgdamageoverhaul", path -> path.getPath().equals("rpgdamageoverhaul/entity_overrides.json")).entrySet())
        {

            try (InputStream stream = manager.getResource(entry.getKey()).get().getInputStream()) {
                InputStreamReader reader = new InputStreamReader(stream);
                JsonObject obj = (JsonObject) JsonParser.parseReader(reader);
                processEntityOverrides(obj);
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}
