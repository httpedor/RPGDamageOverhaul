package com.httpedor.rpgdamageoverhaul;

import com.httpedor.rpgdamageoverhaul.ducktypes.CopyableDefaultAttrContainer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.*;
import java.util.function.BiConsumer;

public class RPGDamageOverhaulAPI {
    static final Map<String, DamageClass> dmgClasses = new HashMap<>();
    static final Map<Identifier, Map<DamageClass, Double>> dmgOverrides = new HashMap<>();
    static final Set<DamageType> rpgDamageTypes = new HashSet<>();
    static final Map<Identifier, TriConsumer<LivingEntity, DamageSource, Double>> onHitEffectCallbacks = new HashMap<>();

    public static DamageClass registerDamage(String dmgName, String parent)
    {
        DamageClass dmgClass;
        if (getDamageClass(dmgName) == null)
        {
            EntityAttribute dmgAttribute = Registry.register(Registries.ATTRIBUTE, "rpgdamageoverhaul:" + dmgName + ".damage", new ClampedEntityAttribute(dmgName + ".damage", 0, 0, 1024));
            EntityAttribute armorAttribute;
            if (!dmgName.equals("physical"))
                armorAttribute = Registry.register(Registries.ATTRIBUTE, "rpgdamageoverhaul:" + dmgName + ".armor", new ClampedEntityAttribute(dmgName + ".armor", 0, 0, 1024));
            else
                armorAttribute = EntityAttributes.GENERIC_ARMOR;
            EntityAttribute absorptionAttribute = Registry.register(Registries.ATTRIBUTE, "rpgdamageoverhaul:" + dmgName + ".absorption", new ClampedEntityAttribute(dmgName + ".absorption", 0, 0, 1024));
            EntityAttribute resistanceAttribute = Registry.register(Registries.ATTRIBUTE, "rpgdamageoverhaul:" + dmgName + ".resistance", new ClampedEntityAttribute(dmgName + ".resistance", 0, -10, 10));
            RegistryKey<DamageType> dmgTypeKey = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, new Identifier("rpgdamageoverhaul", dmgName));
            dmgClass = new DamageClass(dmgName, dmgAttribute, armorAttribute, absorptionAttribute, resistanceAttribute, dmgTypeKey, parent);

            //Add attribute to all entities
            for (var etEntry : Registries.ENTITY_TYPE.getEntrySet())
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

                ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
                    var reg = server.getCombinedDynamicRegistries().getCombinedRegistryManager().get(RegistryKeys.DAMAGE_TYPE);

                    if (reg.contains(dmgClass.damageType))
                        return;
                    //Register damage type
                    DamageType dt = new DamageType(dmgClass.name, 1.0f);
                    Registry.register(reg, "rpgdamageoverhaul:" + dmgClass.name, dt);
                    RPGDamageOverhaulAPI.rpgDamageTypes.add(dt);

                    //This is to make sure that I can call LivingEntity#damage with as many damage types as I want in the same tick
                    reg.populateTags(Map.of(
                            TagKey.of(reg.getKey(), new Identifier("minecraft:bypasses_cooldown")), List.of(reg.getEntry(dt)),
                            TagKey.of(reg.getKey(), new Identifier("minecraft:bypasses_armor")), List.of(reg.getEntry(dt))
                    ));
                });
            }
        }
        else
            dmgClass = getDamageClass(dmgName);

        dmgClasses.put(dmgName, dmgClass);

        System.out.println("Registered damage class: " + dmgName);

        return dmgClass;
    }

    public static DamageClass registerDamage(String dmgName)
    {
        return registerDamage(dmgName, null);
    }

    public static boolean isRPGDamageType(DamageType type)
    {
        return rpgDamageTypes.contains(type);
    }

    public static void registerOverride(Identifier mcDamageType, Map<DamageClass, Double> overrides)
    {
        dmgOverrides.put(mcDamageType, overrides);
    }

    public static Map<DamageClass, Double> getOverrides(Identifier mcDamageType)
    {
        return dmgOverrides.getOrDefault(mcDamageType, null);
    }

    public static Map<DamageClass, Double> getOverrides(DamageSource source)
    {
        return getOverrides(source.getTypeRegistryEntry().getKey().get().getValue());
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

    public static void registerOnHitEffect(Identifier id, TriConsumer<LivingEntity, DamageSource, Double> callback)
    {
        onHitEffectCallbacks.put(id, callback);
    }
}
