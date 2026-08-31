package com.httpedor.rpgdamageoverhaul.api;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.gson.JsonElement;
import com.httpedor.rpgdamageoverhaul.AttributeUtils;
import com.httpedor.rpgdamageoverhaul.Constants;
import com.httpedor.rpgdamageoverhaul.RPGDOLoader;
import com.httpedor.rpgdamageoverhaul.api.DamageClass.DCAttribute;
import com.httpedor.rpgdamageoverhaul.damageproperties.HasTagsProperty;
import com.httpedor.rpgdamageoverhaul.damageproperties.IsDamageTypeProperty;
import com.httpedor.rpgdamageoverhaul.damageproperties.ParentProperty;
import com.httpedor.rpgdamageoverhaul.damageproperties.RenameAttributesProperty;
import com.httpedor.rpgdamageoverhaul.damageproperties.SimpleProperty;
import com.httpedor.rpgdamageoverhaul.ducktypes.CopyableDefaultAttrContainer;
import com.httpedor.rpgdamageoverhaul.platform.Services;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class RPGDamageOverhaulAPI {
    static final Map<String, DamageClass> dmgClasses = new HashMap<>();
    static final Map<ResourceLocation, Map<DamageClass, Double>> dmgOverrides = new HashMap<>();
    static final Set<String> rpgDamageTypes = new HashSet<>();
    static final Map<ResourceLocation, Map<DamageClass, Double>> itemOverrides = new HashMap<>();
    static final Map<ResourceLocation, Map<DamageClass, Double>> tagItemOverrides = new HashMap<>();
    static final Map<ResourceLocation, DamageClass[]> betterCombatAttacks = new HashMap<>();
    static final Map<ResourceLocation, Map<DamageClass, Double>> entityOverrides = new HashMap<>();
    static final Map<ResourceLocation, Map<DamageClass, Double>> tagEntityOverrides = new HashMap<>();
    static final Map<String, Function<JsonElement, DamageClassProperty>> damageClassPropertyBuilders = new HashMap<>();
    static final List<Supplier<DamageClassProperty>> defaultProperties = new ArrayList<>();

    /** Suffix a damage class attribute's registry id ends with, e.g. {@code rpgdamageoverhaul:fire.resistance}. */
    public static String getAttributeSuffix(DCAttribute attr)
    {
        return switch (attr) {
            case DAMAGE -> "damage";
            case ARMOR -> "armor";
            case ABSORPTION -> "absorption";
            case RESISTANCE -> "resistance";
        };
    }

    private static DCAttribute getAttributeKind(ResourceLocation id)
    {
        int dot = id.getPath().lastIndexOf('.');
        if (dot < 0)
            return null;
        return switch (id.getPath().substring(dot + 1)) {
            case "damage" -> DCAttribute.DAMAGE;
            case "armor" -> DCAttribute.ARMOR;
            case "absorption" -> DCAttribute.ABSORPTION;
            case "resistance" -> DCAttribute.RESISTANCE;
            default -> null;
        };
    }

    /** Registers one damage class attribute, or returns the holder already in the registry under that id. */
    public static Holder<Attribute> registerDamageClassAttribute(ResourceLocation id, DCAttribute kind)
    {
        var existing = BuiltInRegistries.ATTRIBUTE.getHolder(id).orElse(null);
        if (existing != null)
            return existing;

        var attribute = kind == DCAttribute.RESISTANCE
                ? new RangedAttribute(id.getPath(), 0, -10, 10)
                : new RangedAttribute(id.getPath(), 0, 0, 1024);
        return Registry.registerForHolder(BuiltInRegistries.ATTRIBUTE, id, attribute);
    }

    public static Map<DCAttribute, Holder<Attribute>> registerDamageClassAttributes(String dmgName)
    {
        Map<DCAttribute, Holder<Attribute>> attributes = new EnumMap<>(DCAttribute.class);
        for (var kind : DCAttribute.values())
        {
            var id = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, dmgName + "." + getAttributeSuffix(kind));
            attributes.put(kind, registerDamageClassAttribute(id, kind));
        }
        return attributes;
    }

    /**
     * Registers every attribute id the server reported, damage class or not.
     * <p>
     * The set has to come from the server's ATTRIBUTE registry rather than from its list of damage classes: the
     * registry keeps the attributes of a class that a /reload later removed or renamed, those ghosts still appear
     * in NeoForge's registry sync snapshot, and applySnapshot disconnects any client missing an entry the snapshot
     * names. Registering them keeps the client's set a superset of the server's, which is all the sync requires.
     */
    public static List<Holder<Attribute>> registerSyncedAttributes(Collection<ResourceLocation> ids)
    {
        List<Holder<Attribute>> holders = new ArrayList<>();
        for (var id : ids)
        {
            var kind = getAttributeKind(id);
            if (kind == null)
            {
                Constants.LOG.warn("Server sent an attribute we can't classify, ignoring it: {}", id);
                continue;
            }
            holders.add(registerDamageClassAttribute(id, kind));
        }
        return holders;
    }

    /**
     * Adds the given attributes to the default AttributeSupplier of every living entity type.
     * <p>
     * An entity's AttributeMap captures its AttributeSupplier once, in the LivingEntity constructor, and never
     * refreshes it. Anything built before this runs will therefore never know about these attributes, and reading
     * one off it throws "Can't find attribute". That makes the call order load-bearing: on a client joining a
     * dedicated server this has to happen during the configuration phase, because the LocalPlayer is created from
     * the login packet, which the server sends before OnDatapackSyncEvent.
     */
    public static void injectAttributesIntoEntityTypes(Collection<Holder<Attribute>> attributes)
    {
        if (attributes.isEmpty())
            return;

        for (var entityType : BuiltInRegistries.ENTITY_TYPE)
        {
            try {
                if (!DefaultAttributes.hasSupplier(entityType))
                    continue;
                @SuppressWarnings("unchecked")
                var type = (EntityType<? extends LivingEntity>) entityType;
                var existingAttrs = DefaultAttributes.getSupplier(type);
                var builder = new AttributeSupplier.Builder();
                for (var attribute : attributes)
                    builder.add(attribute);
                ((CopyableDefaultAttrContainer) existingAttrs).copyTo(builder);

                Services.PLATFORM.updateEntityAttributes(type, builder);
            } catch (ClassCastException ignored) {

            }
        }
    }

    public static DamageClass registerDamageClass(String dmgName, Collection<DamageClassProperty> properties, RegistryAccess ra)
    {

        DamageClass dmgClass;
        if (getDamageClass(dmgName) == null)
        {

            rpgDamageTypes.add("rpgdamageoverhaul:" + dmgName);

            Registry<DamageType> reg = ra.registryOrThrow(Registries.DAMAGE_TYPE);
            ResourceKey<DamageType> dmgTypeKey = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("rpgdamageoverhaul", dmgName));
            var opt = reg.getHolder(dmgTypeKey);
            Holder<DamageType> damageType;
            if (opt.isEmpty())
            {
                var dt = new DamageType(dmgName, 1.0f);
                damageType = Registry.registerForHolder(reg, dmgTypeKey.location(), dt);
            }
            else
                damageType = opt.get();
            dmgClass = new DamageClass(dmgName, registerDamageClassAttributes(dmgName), damageType, properties);
            for (var property : defaultProperties)
            {
                var prop = property.get();
                if (prop instanceof IIdentifiableDamageClassProperty idDcp && !dmgClass.hasPropertyWithId(idDcp.getId()))
                    dmgClass.addProperty(prop);
                else if (!dmgClass.hasPropertyOfType(prop.getClass()))
                    dmgClass.addProperty(prop);
            }

            List<Holder<Attribute>> dcAttributes = new ArrayList<>();
            for (var dcAttr : DCAttribute.values())
                dcAttributes.add(dmgClass.getAttribute(dcAttr));
            injectAttributesIntoEntityTypes(dcAttributes);

            Constants.LOG.info("Registered damage class: {}", dmgName);
        }
        else
        {
            Constants.LOG.warn("Overwriting damage class: {}", dmgName);
            dmgClass = getDamageClass(dmgName);
        }

        dmgClasses.put(dmgName, dmgClass);


        for (var property : dmgClass.getProperties())
            property.onRegistered(dmgClass);

        return dmgClass;
    }

    public static void registerPropertyBuilder(String propertyName, Function<JsonElement, DamageClassProperty> builder)
    {
        damageClassPropertyBuilders.put(propertyName, builder);
    }
    public static void registerPropertyAlias(String propertyName, String alias)
    {
        var builder = damageClassPropertyBuilders.get(propertyName);
        if (builder != null)
            registerPropertyBuilder(alias, builder);
    }
    public static void registerDefaultProperty(Supplier<DamageClassProperty> propertySupplier)
    {
        defaultProperties.add(propertySupplier);
    }
    public static void registerPropertyBuilder(String propertyName, Function<JsonElement, DamageClassProperty> builder, Supplier<DamageClassProperty> defaultProperty)
    {
        registerPropertyBuilder(propertyName, builder);
        registerDefaultProperty(defaultProperty);
    }
    public static Function<JsonElement, DamageClassProperty> getPropertyBuilder(String propertyName)
    {
        return damageClassPropertyBuilders.getOrDefault(propertyName, null);
    }
    public static DamageClassProperty buildProperty(String propertyName, JsonElement json)
    {
        var builder = getPropertyBuilder(propertyName);
        if (builder == null)
        {
            Constants.LOG.error("No builder found for damage class property {}", propertyName);
            return null;
        }
        try {
            return builder.apply(json);
        } catch (Exception e)
        {
            Constants.LOG.error("Error building damage class property {}: {}", propertyName, e.getMessage());
            return null;
        }
    }

    public static boolean isRPGDamageType(Holder<DamageType> type)
    {
        return rpgDamageTypes.contains(type.unwrapKey().get().location().toString());
    }

    public static boolean isRPGDamageType(ResourceLocation id)
    {
        return rpgDamageTypes.contains(id.toString());
    }

    public static Set<String> getRPGDamageTypes()
    {
        return Set.copyOf(rpgDamageTypes);
    }

    public static void registerOverride(ResourceLocation mcDamageType, Map<DamageClass, Double> overrides)
    {
        dmgOverrides.put(mcDamageType, overrides);
    }

    public static Map<DamageClass, Double> getDamageOverrides(ResourceLocation mcDamageType)
    {
        return dmgOverrides.getOrDefault(mcDamageType, null);
    }

    public static Map<DamageClass, Double> getDamageOverrides(DamageSource source)
    {
        return getDamageOverrides(source.typeHolder().unwrapKey().get().location());
    }

    public static void registerItemOverrides(ResourceLocation itemId, Map<DamageClass, Double> overrides)
    {
        itemOverrides.put(itemId, overrides);
    }
    public static void registerItemTagOverrides(ResourceLocation tagId, Map<DamageClass, Double> overrides)
    {
        tagItemOverrides.put(tagId, overrides);
    }

    public static void registerEntityOverrides(ResourceLocation entityId, Map<DamageClass, Double> overrides)
    {
        entityOverrides.put(entityId, overrides);
    }

    public static void registerEntityTagOverrides(ResourceLocation tagId, Map<DamageClass, Double> overrides)
    {
        tagEntityOverrides.put(tagId, overrides);
    }

    public static Map<DamageClass, Double> getItemOverrides(ResourceLocation itemId)
    {
        return itemOverrides.getOrDefault(itemId, null);
    }
    public static Map<DamageClass, Double> getItemTagOverrides(ResourceLocation tagId)
    {
        return tagItemOverrides.getOrDefault(tagId, null);
    }
    public static Map<DamageClass, Double> getItemOverrides(Item item)
    {
        var ret = getItemOverrides(BuiltInRegistries.ITEM.getKey(item));
        if (ret == null)
            ret = new HashMap<>();
        for (var entry : tagItemOverrides.entrySet())
        {
            if (new ItemStack(item).is(TagKey.create(Registries.ITEM, entry.getKey())))
                ret.putAll(entry.getValue());
        }

        return ret;
    }
    public static Map<DamageClass, Double> getItemOverrides(ItemStack is)
    {
        return getItemOverrides(is.getItem());
    }

    public static void applyItemOverrides(ItemStack is, Map<DamageClass, Double> newDamages, double extraDmg) {
        var itemOverrides = RPGDamageOverhaulAPI.getItemOverrides(is);
        if (!itemOverrides.isEmpty())
        {
            double itemDmg = extraDmg;
            final List<Double> modifiers = new ArrayList<>();
            is.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
                if (attribute.equals(Attributes.ATTACK_DAMAGE) && modifier.operation() == AttributeModifier.Operation.ADD_VALUE)
                    modifiers.add(modifier.amount());
            });
            for (double modifier : modifiers)
                itemDmg += modifier;
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

    public static Map<DamageClass, Double> getEntityOverrides(ResourceLocation entityId)
    {
        return entityOverrides.getOrDefault(entityId, new HashMap<>());
    }

    public static Map<DamageClass, Double> getEntityTagOverrides(ResourceLocation tagId)
    {
        return tagEntityOverrides.getOrDefault(tagId, new HashMap<>());
    }

    public static Map<DamageClass, Double> getEntityOverrides(Entity entity)
    {
        HashMap<DamageClass, Double> ret = new HashMap<>();

        for (var entry : tagEntityOverrides.entrySet())
        {
            if (entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, entry.getKey())))
                ret.putAll(entry.getValue());
        }

        var ent = getEntityOverrides(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
        if (ent != null)
            ret.putAll(ent);

        return ret;
    }

   @SuppressWarnings("deprecation")
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

        for (var tag : mob.getType().builtInRegistryHolder().tags().toList())
        {
            entityOverrides = RPGDamageOverhaulAPI.getEntityTagOverrides(tag.location());
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

    public static void registerBetterCombatAttackOverrides(ResourceLocation id, DamageClass[] damages)
    {
        betterCombatAttacks.put(id, damages);
    }

    public static Map<ResourceLocation, DamageClass[]> getAllBetterCombatAttackOverrides()
    {
        return Map.copyOf(betterCombatAttacks);
    }

    public static DamageClass[] getBetterCombatAttackOverrides(ResourceLocation id)
    {
        return betterCombatAttacks.getOrDefault(id, null);
    }

    public static void unloadEverything()
    {
        dmgClasses.clear();
        dmgOverrides.clear();
        rpgDamageTypes.clear();
        itemOverrides.clear();
        tagItemOverrides.clear();
        betterCombatAttacks.clear();
        entityOverrides.clear();
        tagEntityOverrides.clear();

        // Properties keep their own static caches, populated from onRegistered. They have to be dropped here too,
        // or state from the previous pack survives into the next one -- see each clear() for what that breaks.
        RenameAttributesProperty.clear();
        IsDamageTypeProperty.clear();
        HasTagsProperty.clear();
        ParentProperty.clear();
    }

    public static TextColor getDamageClassColor(DamageClass dc)
    {
        return getDamageClassColor(dc, TextColor.fromLegacyFormat(ChatFormatting.WHITE));
    }
    public static TextColor getDamageClassColor(DamageClass dc, TextColor def)
    {
        TextColor color = dc.getSimplePropertyById("color");
        if (color == null)
            color = def;

        return color;
    }

    public static Map<DamageSource, Double> applyDamageOverrides(DamageSource source, float amount)
    {
        if (RPGDamageOverhaulAPI.isRPGDamageType(source.typeHolder()))
            return null;

        Map<DamageClass, Double> overrides = RPGDamageOverhaulAPI.getDamageOverrides(source);
        if (overrides == null || overrides.isEmpty())
            return null;

        Map<DamageSource, Double> newDmgs = new HashMap<>();

        for (Map.Entry<DamageClass, Double> entry : overrides.entrySet()) {
            DamageClass dmgClass = entry.getKey();
            double dmg = entry.getValue();
            if (dmgClass == null)
                continue;

            DamageSource newSource = dmgClass.createDamageSource(source, true);
            double newDamage = amount * dmg;
            // The reasoning for this being removed is at PlayerMixin. If the mod that adds a certain attack doesn't add
            //an attribute that RPGDO can use to scale that damage, then it's probably balanced in a way that would be
            //messed up if RPGDO increased that damage further.
            /*if (source.getEntity() != null && source.getEntity() instanceof LivingEntity le)
            {
                var attrInstance = le.getAttribute(dmgClass.getAttribute(DCAttribute.DAMAGE));
                if (attrInstance != null)
                    newDamage = AttributeUtils.applyAttributeModifiers((float)newDamage, attrInstance.getModifiers());
            }*/
            newDmgs.put(newSource, newDamage);
        }

        return newDmgs;
    }

}
