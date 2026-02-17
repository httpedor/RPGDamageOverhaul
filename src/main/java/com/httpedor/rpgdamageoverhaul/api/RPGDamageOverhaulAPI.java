package com.httpedor.rpgdamageoverhaul.api;

import com.httpedor.rpgdamageoverhaul.RPGDamageOverhaul;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.*;

public class RPGDamageOverhaulAPI {
    static final Map<String, DamageClass> dmgClasses = new HashMap<>();
    static final Map<ResourceLocation, Map<DamageClass, Double>> dmgOverrides = new HashMap<>();
    static final Set<String> rpgDamageTypes = new HashSet<>();
    static final Map<ResourceLocation, TriConsumer<LivingEntity, DamageSource, Double>> onHitEffectCallbacks = new HashMap<>();
    static final Map<ResourceLocation, Map<DamageClass, Double>> itemOverrides = new HashMap<>();
    static final Map<ResourceLocation, Map<DamageClass, Double>> tagItemOverrides = new HashMap<>();
    static final Map<ResourceLocation, DamageClass[]> betterCombatAttacks = new HashMap<>();
    static final Map<ResourceLocation, Map<DamageClass, Double>> entityOverrides = new HashMap<>();
    static final Map<ResourceLocation, Map<DamageClass, Double>> tagEntityOverrides = new HashMap<>();

    public static final LinkedList<DamageClass> missingDamageTypes = new LinkedList<>();

    private static volatile Map<EntityType<? extends LivingEntity>, AttributeSupplier> FORGE_ATTRIBUTES_MAP;
    private static volatile boolean FORGE_ATTRIBUTES_LOOKUP_FAILED;

    public record DamageClassAttributes(String dmg, String armor, String absorption, String resistance) {}

    @SuppressWarnings("unchecked")
    private static Map<EntityType<? extends LivingEntity>, AttributeSupplier> getForgeAttributesMap() {
        if (FORGE_ATTRIBUTES_LOOKUP_FAILED) {
            return null;
        }
        var cached = FORGE_ATTRIBUTES_MAP;
        if (cached != null) {
            return cached;
        }
        try {
            var field = ForgeHooks.class.getDeclaredField("FORGE_ATTRIBUTES");
            field.setAccessible(true);
            var value = field.get(null);
            if (value instanceof Map<?, ?> map) {
                FORGE_ATTRIBUTES_MAP = (Map<EntityType<? extends LivingEntity>, AttributeSupplier>) map;
                return FORGE_ATTRIBUTES_MAP;
            }
        } catch (Throwable t) {
            RPGDamageOverhaul.LOGGER.error("Failed to access ForgeHooks.FORGE_ATTRIBUTES via reflection; entity attributes may not include RPGDamageOverhaul attributes until next restart.", t);
        }
        FORGE_ATTRIBUTES_LOOKUP_FAILED = true;
        return null;
    }

    @SuppressWarnings("rawtypes")
    public static DamageClass registerDamage(String dmgName, String parent, DamageClassAttributes attr, RegistryAccess ra)
    {
        var wasFrozen = ((ForgeRegistry)ForgeRegistries.ATTRIBUTES).isLocked();
        ((ForgeRegistry)ForgeRegistries.ATTRIBUTES).unfreeze();
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
            Attribute dmgAttribute = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(attr.dmg));
            if (dmgAttribute == null)
            {
                dmgAttribute = new RangedAttribute(dmgName + ".damage", 0, 0, 1024);
                ForgeRegistries.ATTRIBUTES.register(new ResourceLocation(attr.dmg), dmgAttribute);
            }
            Attribute armorAttribute = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(attr.armor));
            if (armorAttribute == null)
            {
                armorAttribute = new RangedAttribute(dmgName + ".armor", 0, 0, 1024);
                ForgeRegistries.ATTRIBUTES.register(new ResourceLocation(attr.armor), armorAttribute);
            }
            Attribute absorptionAttribute = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(attr.absorption));
            if (absorptionAttribute == null)
            {
                absorptionAttribute = new RangedAttribute(dmgName + ".absorption", 0, 0, 1024);
                ForgeRegistries.ATTRIBUTES.register(new ResourceLocation(attr.absorption), absorptionAttribute);
            }
            Attribute resistanceAttribute = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(attr.resistance));
            if (resistanceAttribute == null)
            {
                resistanceAttribute = new RangedAttribute(dmgName + ".resistance", 0, -10, 10);
                ForgeRegistries.ATTRIBUTES.register(new ResourceLocation(attr.resistance), resistanceAttribute);
            }

            Optional<Registry<DamageType>> reg = Optional.empty();
            if (ra != null)
                reg = ra.registry(Registries.DAMAGE_TYPE);
            dmgClass = new DamageClass(dmgName, dmgAttribute, armorAttribute, absorptionAttribute, resistanceAttribute, null, parent);
            rpgDamageTypes.add("rpgdamageoverhaul:" + dmgName);
            if (reg.isPresent())
                tryRegisterDamageType(dmgClass, reg.get());
            else
                missingDamageTypes.add(dmgClass);
            RPGDamageOverhaul.LOGGER.info("Registered damage class: {}", dmgName);
        }
        else
        {
            RPGDamageOverhaul.LOGGER.warn("Overwriting damage class: {}", dmgName);
            dmgClass = getDamageClass(dmgName);
        }

        dmgClasses.put(dmgName, dmgClass);

        for (var entityType: ForgeRegistries.ENTITY_TYPES.getValues())
        {
            try {
                if (!DefaultAttributes.hasSupplier(entityType))
                    continue;
                var existingAttrs = DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) entityType);
                var builder = new AttributeSupplier.Builder(existingAttrs);
                builder.add(dmgClass.dmgAttribute);
                builder.add(dmgClass.armorAttribute);
                builder.add(dmgClass.absorptionAttribute);
                builder.add(dmgClass.resistanceAttribute);

                var forgeAttrs = getForgeAttributesMap();
                if (forgeAttrs != null) {
                    forgeAttrs.put((EntityType<? extends LivingEntity>) entityType, builder.build());
                }
            } catch (ClassCastException ignored) {

            }
        }

        if (wasFrozen)
            ((ForgeRegistry)ForgeRegistries.ATTRIBUTES).freeze();
        return dmgClass;
    }

    public static DamageClass registerDamage(String dmgName, String parent, RegistryAccess ra)
    {
        return registerDamage(dmgName, parent, null, ra);
    }

    public static DamageClass registerDamage(String dmgName, RegistryAccess ra)
    {
        return registerDamage(dmgName, null, null, ra);
    }

    public static boolean isRPGDamageType(Holder<DamageType> type)
    {
        return rpgDamageTypes.contains(type.unwrapKey().get().location());
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
        var ret = getItemOverrides(ForgeRegistries.ITEMS.getKey(item));
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
            for (var mod : is.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE))
                itemDmg += mod.getAmount();
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

        var ent = getEntityOverrides(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()));
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

        for (var tag : mob.getType().getTags().toList())
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

    public static void registerOnHitEffect(ResourceLocation id, TriConsumer<LivingEntity, DamageSource, Double> callback)
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
        entityOverrides.clear();
    }

    public static TextColor getDamageClassColor(DamageClass dc)
    {
        return getDamageClassColor(dc, TextColor.fromLegacyFormat(ChatFormatting.WHITE));
    }
    public static TextColor getDamageClassColor(DamageClass dc, TextColor def)
    {
        TextColor color = null;
        if (dc.properties.containsKey("color"))
        {
            var dcColor = dc.properties.get("color").getAsString().toLowerCase();
            color = TextColor.parseColor(dcColor);

            if (color == null)
                RPGDamageOverhaul.LOGGER.error("Failed to find color {} for damage class {}", dcColor, dc.name);
        }
        if (color == null)
            color = def;

        return color;
    }
    
    public static void tryRegisterDamageType(DamageClass dc, Registry<DamageType> reg)
    {
        ResourceKey<DamageType> dmgTypeKey = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("rpgdamageoverhaul", dc.name));
        var holder = reg.getHolder(dmgTypeKey);
        if (holder.isEmpty())
        {
            var dt = new DamageType(dc.name, 1.0f);
            dc.damageType = Registry.registerForHolder(reg, dmgTypeKey.location(), dt);
        }
        else
            dc.damageType = holder.get();
    }
}
