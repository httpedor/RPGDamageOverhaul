package com.httpedor.rpgdamageoverhaul.api;

import com.httpedor.rpgdamageoverhaul.RPGDamageOverhaul;
import com.httpedor.rpgdamageoverhaul.ducktypes.CopyableDefaultAttrContainer;
import com.httpedor.rpgdamageoverhaul.events.DamageClassRegisteredCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.*;

public class RPGDamageOverhaulAPI {
    static final Map<String, DamageClass> dmgClasses = new HashMap<>();
    static final Map<Identifier, Map<DamageClass, Double>> dmgOverrides = new HashMap<>();
    static final Set<String> rpgDamageTypes = new HashSet<>();
    static final Map<Identifier, TriConsumer<LivingEntity, DamageSource, Double>> onHitEffectCallbacks = new HashMap<>();
    static final Map<Identifier, Map<DamageClass, Double>> itemOverrides = new HashMap<>();
    static final Map<Identifier, Map<DamageClass, Double>> tagItemOverrides = new HashMap<>();
    static final Map<Identifier, DamageClass[]> betterCombatAttacks = new HashMap<>();
    static final Map<Identifier, Map<DamageClass, Double>> entityOverrides = new HashMap<>();
    static final Map<Identifier, Map<DamageClass, Double>> tagEntityOverrides = new HashMap<>();

    public record DamageClassAttributes(String dmg, String armor, String absorption, String resistance) {}

    public static DamageClass registerDamage(String dmgName, String parent, DamageClassAttributes attr)
    {
        if (attr == null)
        {
            attr = new DamageClassAttributes("rpgdamageoverhaul:" + dmgName + ".damage",
            "rpgdamageoverhaul:" + dmgName + ".armor",
            "rpgdamageoverhaul:" + dmgName + ".absorption",
           "rpgdamageoverhaul:" + dmgName + ".resistance");
        }
        DamageClass dmgClass;
        if (getDamageClass(dmgName) == null)
        {
            EntityAttribute dmgAttribute = Registries.ATTRIBUTE.get(new Identifier(attr.dmg));
            if (dmgAttribute == null)
                dmgAttribute = Registry.register(Registries.ATTRIBUTE, attr.dmg, new ClampedEntityAttribute(dmgName + ".damage", 0, 0, 1024));
            EntityAttribute armorAttribute = Registries.ATTRIBUTE.get(new Identifier(attr.armor));
            if (armorAttribute == null)
                armorAttribute = Registry.register(Registries.ATTRIBUTE, attr.armor, new ClampedEntityAttribute(dmgName + ".armor", 0, 0, 1024));
            EntityAttribute absorptionAttribute = Registries.ATTRIBUTE.get(new Identifier(attr.absorption));
            if (absorptionAttribute == null)
                absorptionAttribute = Registry.register(Registries.ATTRIBUTE, attr.absorption, new ClampedEntityAttribute(dmgName + ".absorption", 0, 0, 1024));
            EntityAttribute resistanceAttribute = Registries.ATTRIBUTE.get(new Identifier(attr.resistance));
            if (resistanceAttribute == null)
                resistanceAttribute = Registry.register(Registries.ATTRIBUTE, attr.resistance, new ClampedEntityAttribute(dmgName + ".resistance", 0, -10, 10));

            RegistryKey<DamageType> dmgTypeKey = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, new Identifier("rpgdamageoverhaul", dmgName));
            dmgClass = new DamageClass(dmgName, dmgAttribute, armorAttribute, absorptionAttribute, resistanceAttribute, dmgTypeKey, parent);

            ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
                var reg = server.getCombinedDynamicRegistries().getCombinedRegistryManager().get(RegistryKeys.DAMAGE_TYPE);

                DamageType dt = new DamageType(dmgClass.name, 1.0f);
                if (!reg.contains(dmgClass.damageType))
                    Registry.register(reg, "rpgdamageoverhaul:" + dmgClass.name, dt);
                //Register damage type
                RPGDamageOverhaulAPI.rpgDamageTypes.add(dt.msgId());

                dmgClass.damageTypeEntry = reg.getEntry(dt);
            });

            //Add attribute to all entities
            for (var etEntry : Registries.ENTITY_TYPE.getEntrySet())
            {
                try
                {
                    var entityType = (EntityType<? extends LivingEntity>)etEntry.getValue();
                    var attrContainer = DefaultAttributeRegistry.get(entityType);
                    if (attrContainer == null)
                        continue;

                    var builder = DefaultAttributeContainer.builder();
                    ((CopyableDefaultAttrContainer)attrContainer).copyTo(builder);
                    builder.add(dmgAttribute);
                    builder.add(armorAttribute);
                    builder.add(absorptionAttribute);
                    builder.add(resistanceAttribute);

                    FabricDefaultAttributeRegistry.register(entityType, builder);
                } catch (ClassCastException ignored) {

                }
            }
        }
        else
            dmgClass = getDamageClass(dmgName);

        dmgClasses.put(dmgName, dmgClass);
        DamageClassRegisteredCallback.EVENT.invoker().interact(dmgClass);

        RPGDamageOverhaul.LOGGER.info("Registered damage class: {}", dmgName);

        return dmgClass;
    }

    public static DamageClass registerDamage(String dmgName, String parent)
    {
        return registerDamage(dmgName, parent, null);
    }

    public static DamageClass registerDamage(String dmgName)
    {
        return registerDamage(dmgName, null, null);
    }

    public static boolean isRPGDamageType(DamageType type)
    {
        return rpgDamageTypes.contains(type.msgId());
    }

    public static boolean isRPGDamageType(String name)
    {
        return rpgDamageTypes.contains(name);
    }

    public static Set<String> getRPGDamageTypes()
    {
        return Set.copyOf(rpgDamageTypes);
    }

    public static void registerOverride(Identifier mcDamageType, Map<DamageClass, Double> overrides)
    {
        dmgOverrides.put(mcDamageType, overrides);
    }

    public static Map<DamageClass, Double> getDamageOverrides(Identifier mcDamageType)
    {
        return dmgOverrides.getOrDefault(mcDamageType, null);
    }

    public static Map<DamageClass, Double> getDamageOverrides(DamageSource source)
    {
        return getDamageOverrides(source.getTypeRegistryEntry().getKey().get().getValue());
    }

    public static void registerItemOverrides(Identifier itemId, Map<DamageClass, Double> overrides)
    {
        itemOverrides.put(itemId, overrides);
    }
    public static void registerItemTagOverrides(Identifier tagId, Map<DamageClass, Double> overrides)
    {
        tagItemOverrides.put(tagId, overrides);
    }

    public static void registerEntityOverrides(Identifier entityId, Map<DamageClass, Double> overrides)
    {
        entityOverrides.put(entityId, overrides);
    }

    public static void registerEntityTagOverrides(Identifier tagId, Map<DamageClass, Double> overrides)
    {
        tagEntityOverrides.put(tagId, overrides);
    }

    public static Map<DamageClass, Double> getItemOverrides(Identifier itemId)
    {
        return itemOverrides.getOrDefault(itemId, null);
    }
    public static Map<DamageClass, Double> getItemTagOverrides(Identifier tagId)
    {
        return tagItemOverrides.getOrDefault(tagId, null);
    }
    public static Map<DamageClass, Double> getItemOverrides(Item item)
    {
        var ret = getItemOverrides(Registries.ITEM.getId(item));
        if (ret == null)
        {
            for (var entry : tagItemOverrides.entrySet())
            {
                if (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, entry.getKey())))
                    return entry.getValue();
            }
        }
        return ret;
    }
    public static Map<DamageClass, Double> getItemOverrides(ItemStack is)
    {
        return getItemOverrides(is.getItem());
    }

    public static void applyItemOverrides(ItemStack is, Map<DamageClass, Double> newDamages, double extraDmg) {
        var itemOverrides = RPGDamageOverhaulAPI.getItemOverrides(is);
        if (itemOverrides != null)
        {
            double itemDmg = extraDmg;
            for (var mod : is.getAttributeModifiers(EquipmentSlot.MAINHAND).get(EntityAttributes.GENERIC_ATTACK_DAMAGE))
                itemDmg += mod.getValue();
            for (var entry : itemOverrides.entrySet())
            {
                var dc = entry.getKey();
                var multiplier = entry.getValue();
                if (multiplier > 0)
                    newDamages.put(dc, newDamages.getOrDefault(dc, 0d) + (multiplier * itemDmg));
            }
        }
    }

    public static void applyItemOverrides(ItemStack is, Map<DamageClass, Double> newDamages)
    {
        applyItemOverrides(is, newDamages, 0);
    }

    public static Map<DamageClass, Double> getEntityOverrides(Identifier entityId)
    {
        return entityOverrides.getOrDefault(entityId, null);
    }

    public static Map<DamageClass, Double> getEntityTagOverrides(Identifier tagId)
    {
        return tagEntityOverrides.getOrDefault(tagId, null);
    }

    public static Map<DamageClass, Double> getEntityOverrides(Entity entity)
    {
        HashMap<DamageClass, Double> ret = new HashMap<>();

        for (var entry : tagEntityOverrides.entrySet())
        {
            if (entity.getType().isIn(TagKey.of(RegistryKeys.ENTITY_TYPE, entry.getKey())))
                ret.putAll(entry.getValue());
        }

        var ent = getEntityOverrides(Registries.ENTITY_TYPE.getId(entity.getType()));
        if (ent != null)
            ret.putAll(ent);

        return ret;
    }

    public static void applyEntityOverrides(Entity mob, Map<DamageClass, Double> newDamages, float damage)
    {
        var entityOverrides = RPGDamageOverhaulAPI.getEntityOverrides(mob);
        if (!entityOverrides.isEmpty())
        {
            for (var entry : entityOverrides.entrySet())
            {
                var dc = entry.getKey();
                var multiplier = entry.getValue();
                if (multiplier > 0)
                    newDamages.put(dc, newDamages.getOrDefault(dc, 0d) + (multiplier * damage));
            }
        }

        for (var tag : mob.getType().getRegistryEntry().streamTags().toList())
        {
            entityOverrides = RPGDamageOverhaulAPI.getEntityTagOverrides(tag.id());
            if (!entityOverrides.isEmpty())
            {
                for (var entry : entityOverrides.entrySet())
                {
                    var dc = entry.getKey();
                    var multiplier = entry.getValue();
                    if (multiplier > 0)
                        newDamages.put(dc, newDamages.getOrDefault(dc, 0d) + (multiplier * damage));
                }
            }
        }
    }

    public static DamageClass getDamageClass(String name)
    {
        return dmgClasses.getOrDefault(name, null);
    }

    public static DamageClass getDamageClass(DamageType type)
    {
        return getDamageClass(type.msgId());
    }

    public static Collection<DamageClass> getAllDamageClasses()
    {
        return dmgClasses.values().stream().filter(Objects::nonNull).toList();
    }

    public static void registerBetterCombatAttackOverrides(Identifier id, DamageClass[] damages)
    {
        betterCombatAttacks.put(id, damages);
    }

    public static Map<Identifier, DamageClass[]> getAllBetterCombatAttackOverrides()
    {
        return Map.copyOf(betterCombatAttacks);
    }

    public static DamageClass[] getBetterCombatAttackOverrides(Identifier id)
    {
        return betterCombatAttacks.getOrDefault(id, null);
    }

    public static void registerOnHitEffect(Identifier id, TriConsumer<LivingEntity, DamageSource, Double> callback)
    {
        onHitEffectCallbacks.put(id, callback);
    }

    public static void unloadEverything()
    {
        dmgClasses.clear();
        dmgOverrides.clear();
        rpgDamageTypes.clear();
        itemOverrides.clear();
        tagItemOverrides.clear();
        betterCombatAttacks.clear();
    }
}
