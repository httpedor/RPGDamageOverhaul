package com.httpedor.rpgdamageoverhaul;

import com.google.gson.*;
import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.httpedor.rpgdamageoverhaul.events.DamageClassRegisteredEvent;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageType;
import net.minecraftforge.common.MinecraftForge;

import java.util.*;

public class DatapackLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer()).create();
    public HashMap<String, JsonObject> dcEntries = new HashMap<>();
    public RegistryAccess ra;

    public DatapackLoader() {
        super(GSON, "rpgdamageoverhaul");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsonFiles, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        RPGDamageOverhaulAPI.unloadEverything();
        for (var entry : jsonFiles.entrySet())
        {
            var location = entry.getKey();
            var element = entry.getValue();
            try
            {
                switch (location.getPath())
                {
                    case "damage_classes":
                        processDamageClasses(element.getAsJsonObject());
                        break;
                    case "bettercombat":
                        processBetterCombatOverrides(element.getAsJsonObject());
                        break;
                    case "damage_overrides":
                        processDamageOverrides(element.getAsJsonObject());
                        break;
                    case "item_overrides":
                        processItemOverrides(element.getAsJsonObject());
                        break;
                    case "entity_overrides":
                        processEntityOverrides(element.getAsJsonObject());
                        break;
                }
            } catch (Exception e)
            {
                RPGDamageOverhaul.LOGGER.error("Error loading datapack file: {}", location, e);
            }
        }

        var reg = ra.registry(Registries.DAMAGE_TYPE).get();
        for (DamageClass dc : RPGDamageOverhaulAPI.getAllDamageClasses())
        {
            DamageType dt;
            if (!reg.containsKey(dc.damageTypeKey))
            {
                dt = new DamageType(dc.name, 1.0f);
                Registry.register(reg, "rpgdamageoverhaul:" + dc.name, dt);
            }
            dc.damageType = reg.getHolderOrThrow(dc.damageTypeKey).get();
            RPGDamageOverhaulAPI.reloadDamageType(dc);
        }
    }

    public void registerDamageClass(String name, JsonObject obj, DamageClass parent)
    {
        List<ResourceLocation> onHitEffects = new ArrayList<>();
        if (obj.has("onHit"))
            onHitEffects = obj.getAsJsonArray("onHit").asList().stream().map(JsonElement::getAsString).map(ResourceLocation::new).toList();

        String dmgAttribute = obj.has("damage") ? obj.get("damage").getAsString() : "rpgdamageoverhaul:" + name + "." + "damage";
        String armorAttribute = obj.has("armor") ? obj.get("armor").getAsString() : "rpgdamageoverhaul:" + name + "." + "armor";
        String absorptionAttribute = obj.has("absorption") ? obj.get("absorption").getAsString() : "rpgdamageoverhaul:" + name + "." + "absorption";
        String resistanceAttribute = obj.has("resistance") ? obj.get("resistance").getAsString() : "rpgdamageoverhaul:" + name + "." + "resistance";

        DamageClass dc = RPGDamageOverhaulAPI.registerDamage(name, parent == null ? null : parent.name, new RPGDamageOverhaulAPI.DamageClassAttributes(dmgAttribute, armorAttribute, absorptionAttribute, resistanceAttribute));
        dc.properties = obj.asMap();

        for (ResourceLocation effect : onHitEffects)
        {
            dc.addOnHitEffect(effect);
        }

        if (obj.has("subClasses")) {
            for (Map.Entry<String, JsonElement> subClass : obj.getAsJsonObject("subClasses").entrySet()) {
                registerDamageClass(subClass.getKey(), subClass.getValue().getAsJsonObject(), RPGDamageOverhaulAPI.getDamageClass(name));
            }
        }
        dcEntries.put(name, obj);
        MinecraftForge.EVENT_BUS.post(new DamageClassRegisteredEvent(dc));
    }


    void processDamageClasses(JsonObject obj)
    {
        for (Map.Entry<String, JsonElement> dcJson : obj.entrySet())
        {
            var name = dcJson.getKey();
            registerDamageClass(name, dcJson.getValue().getAsJsonObject(), null);
        }
    }
    void processBetterCombatOverrides(JsonObject obj)
    {
        for (Map.Entry<String, JsonElement> attacksEntry : obj.entrySet())
        {
            JsonArray arr = attacksEntry.getValue().getAsJsonArray();
            DamageClass[] dcs = new DamageClass[arr.size()];
            for (int i = 0; i < arr.size(); i++)
            {
                dcs[i] = RPGDamageOverhaulAPI.getDamageClass(arr.get(i).getAsString());
                if (dcs[i] == null)
                    RPGDamageOverhaul.LOGGER.warn("Damage class not found: {}", arr.get(i).getAsString());
            }
            RPGDamageOverhaulAPI.registerBetterCombatAttackOverrides(new ResourceLocation(attacksEntry.getKey()), dcs);

            Map<DamageClass, Integer> counts = new HashMap<>();
            for (DamageClass dc : dcs) {
                counts.put(dc, counts.getOrDefault(dc, 0) + 1);
            }

            Map<DamageClass, Double> fallbackItemOverrides = new HashMap<>();
            for (Map.Entry<DamageClass, Integer> count : counts.entrySet())
            {
                fallbackItemOverrides.put(count.getKey(), 1.0 / dcs.length * count.getValue());
            }
            RPGDamageOverhaulAPI.registerItemOverrides(new ResourceLocation(attacksEntry.getKey()), fallbackItemOverrides);
        }

    }
    void processDamageOverrides(JsonObject obj)
    {
        for (Map.Entry<String, JsonElement> entry : obj.entrySet())
        {
            ResourceLocation mcDamageType = new ResourceLocation(entry.getKey());
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
            boolean isTag;
            ResourceLocation id;
            if (itemOverride.getKey().startsWith("#"))
            {
                isTag = true;
                id = new ResourceLocation(itemOverride.getKey().substring(1));
            }
            else
            {
                isTag = false;
                id = new ResourceLocation(itemOverride.getKey());
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
            boolean isTag = false;
            ResourceLocation id;
            if (entityOverride.getKey().startsWith("#"))
            {
                isTag = true;
                id = new ResourceLocation(entityOverride.getKey().substring(1));
            }
            else
            {
                id = new ResourceLocation(entityOverride.getKey());
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
}
