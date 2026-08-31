package com.httpedor.rpgdamageoverhaul;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.google.gson.JsonElement;
import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.httpedor.rpgdamageoverhaul.damageproperties.*;
import com.httpedor.rpgdamageoverhaul.damageproperties.onhit.*;
import com.httpedor.rpgdamageoverhaul.platform.Services;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import oshi.util.tuples.Pair;
import s_com.udojava.evalexrpgdo.Expression;

public class DCProperties {
    public static final String WATER_DAMAGE_PROPERTY = "on_water";
    public static final String ARMOR_MODIFIER_PROPERTY = "armor_effectiveness";
    public static final String PARENT_DEFENSE_EFFECTIVENESS_PROPERTY = "parent_defense_effectiveness";
    public static final String PARENT_DAMAGE_DEFENSE_EFFECTIVENESS_PROPERTY = "parent_damage_defense_effectiveness";

    private static Function<Float, Float> compileFormula(JsonElement element, String varName)
    {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber())
        {
            float multiplier = element.getAsFloat();
            return (v) -> v * multiplier;
        }
        else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString())
        {
            Expression exp = new Expression(element.getAsString());
            return (v) -> exp.with(varName, BigDecimal.valueOf(v)).eval().floatValue();
        }
        else
            throw new IllegalArgumentException("Invalid formula: " + element);
    }
    private static Function<Float, Integer> compileFormulaToInt(JsonElement element, String varName)
    {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber())
        {
            float multiplier = element.getAsFloat();
            return (v) -> (int)(v * multiplier);
        }
        else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString())
        {
            Expression exp = new Expression(element.getAsString());
            return (v) -> exp.with(varName, BigDecimal.valueOf(v)).eval().intValue();
        }
        else
            throw new IllegalArgumentException("Invalid formula: " + element);
    }
    private static BiFunction<Float, Float, Float> compileBiFormula(JsonElement element, String var1Name, String var2Name, boolean firstArgumentScaling)
    {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber())
        {
            float multiplier = element.getAsFloat();
            if (firstArgumentScaling)
                return (v1, v2) -> v1 * multiplier;
            else
                return (v1, v2) -> v2 * multiplier;
        }
        else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString())
        {
            Expression exp = new Expression(element.getAsString());
            return (v1, v2) -> exp.with(var1Name, BigDecimal.valueOf(v1)).with(var2Name, BigDecimal.valueOf(v2)).eval().floatValue();
        }
        else
            throw new IllegalArgumentException("Invalid formula: " + element);
    }
    private static Function<Integer, Float> compileFormulaInt(JsonElement element, String varName)
    {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber())
        {
            float multiplier = element.getAsFloat();
            return (v) -> v * multiplier;
        }
        else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString())
        {
            Expression exp = new Expression(element.getAsString());
            return (v) -> exp.with(varName, BigDecimal.valueOf(v)).eval().floatValue();
        }
        else
            throw new IllegalArgumentException("Invalid formula: " + element);
    }
    private static Function<Float, Float> compileFormulaConstantDefault(JsonElement element, String varName)
    {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber())
        {
            float constant = element.getAsFloat();
            return (v) -> constant;
        }
        else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString())
        {
            Expression exp = new Expression(element.getAsString());
            return (v) -> exp.with(varName, BigDecimal.valueOf(v)).eval().floatValue();
        }
        else
            throw new IllegalArgumentException("Invalid formula: " + element);
    }

    public static void init()
    {
        RPGDamageOverhaulAPI.registerPropertyBuilder("color", (element) -> {
            var dcColor = element.getAsString().toLowerCase();
            var result = TextColor.parseColor(dcColor);

            if (result.isError())
                throw new IllegalArgumentException("Invalid color: " + dcColor);
            return new SimpleProperty<>("color", result.getOrThrow());
        });
        RPGDamageOverhaulAPI.registerPropertyBuilder("parent",
            (element) -> new ParentProperty(element.getAsString()),
            () -> new ParentProperty(""));
        RPGDamageOverhaulAPI.registerPropertyBuilder("potions", (element) -> {
            return new PotionAttributesProperty(ModifyAttributesProperty.fromJson(element,
                (str) -> BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.parse(str)),
                (json, id) -> JsonHelpers.parseAttributeModifier(id, json),
                "potion"));
        });
        RPGDamageOverhaulAPI.registerPropertyBuilder("enchantments", (element) -> 
                new EnchantmentAttributesProperty(ModifyAttributesProperty.fromJson(element, 
                        (str) -> ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.parse(str)), 
                        (json, id) -> compileFormulaInt(json, "lvl"),
                        "enchantment")));

        RPGDamageOverhaulAPI.registerPropertyBuilder("damage_types", (element) -> {
            var dts = element.getAsJsonArray().asList();
            List<ResourceLocation> damageTypes = new ArrayList<>();
            for (var damageTypeEl : dts)
                damageTypes.add(ResourceLocation.parse(damageTypeEl.getAsString()));

            return new IsDamageTypeProperty(damageTypes);
        });

        RPGDamageOverhaulAPI.registerPropertyBuilder("tags", (element) -> {
            var tags = element.getAsJsonArray().asList();
            List<ResourceLocation> tagLocations = new ArrayList<>();
            for (var tag : tags)
                tagLocations.add(ResourceLocation.parse(tag.getAsString()));

            return new HasTagsProperty(tagLocations.toArray(new ResourceLocation[0]));
        });
        RPGDamageOverhaulAPI.registerPropertyBuilder(WATER_DAMAGE_PROPERTY, (element) -> 
                new SimpleProperty<>(WATER_DAMAGE_PROPERTY, compileFormula(element, "dmg")));

        RPGDamageOverhaulAPI.registerPropertyBuilder(ARMOR_MODIFIER_PROPERTY, (element) ->
                new SimpleProperty<>(ARMOR_MODIFIER_PROPERTY, compileBiFormula(element, "dmg", "armor", false)));
        RPGDamageOverhaulAPI.registerPropertyBuilder("particle", (element) -> {
            var particleId = ResourceLocation.parse(element.getAsString());
            var particleType = BuiltInRegistries.PARTICLE_TYPE.get(particleId);
            if (particleType == null)
                throw new IllegalArgumentException("Invalid particle: " + particleId);
            if (particleType instanceof SimpleParticleType simpleParticleType)
                return new ParticlesOnHit(simpleParticleType);
            throw new IllegalArgumentException("Particle is not a SimpleParticle: " + particleId);
        });
        RPGDamageOverhaulAPI.registerPropertyBuilder("ignite", (element) -> {
            float duration = element.getAsFloat();
            if (duration < 0)
                throw new IllegalArgumentException("Invalid set_fire duration: " + duration);
            return new FireOnHit(duration);
        });
        RPGDamageOverhaulAPI.registerPropertyBuilder("freeze", (element) -> {
            float duration = element.getAsFloat();
            if (duration < 0)
                throw new IllegalArgumentException("Invalid freeze duration: " + duration);
            return new FreezeOnHit(duration);
        });
        RPGDamageOverhaulAPI.registerPropertyBuilder("modify_heal", (element) -> {
            if (!element.isJsonObject())
                throw new IllegalArgumentException("modify_heal property must be a JSON object");

            var jsonObj = element.getAsJsonObject();
            var modEl = jsonObj.get("modifier");
            if (modEl == null)
                throw new IllegalArgumentException("modify_heal property must have a 'modifier' field");
            BiFunction<Float, Float, Float> mod = compileBiFormula(modEl, "heal", "dmg", true);

            JsonElement durationEl = jsonObj.get("duration");
            if (durationEl == null)
                throw new IllegalArgumentException("modify_heal property must have a 'duration' field");
            Function<Float, Float> duration = compileFormula(durationEl, "dmg");

            return new HealingModifierOnHit(mod, duration);
        });
        RPGDamageOverhaulAPI.registerPropertyBuilder("chain_damage", (element) -> {
            if (!element.isJsonObject())
                throw new IllegalArgumentException("chain_damage property must be a JSON object");

            var jsonObj = element.getAsJsonObject();

            JsonElement maxTargetsEl = jsonObj.get("max_targets");
            if (maxTargetsEl == null)
                throw new IllegalArgumentException("chain_damage property must have a 'max_targets' field");
            Function<Float, Integer> maxTargets = compileFormulaToInt(maxTargetsEl, "dmg");

            BiFunction<Float, Integer, Float> damageModifier;
            JsonElement damageModifierEl = jsonObj.get("damage_modifier");
            if (damageModifierEl == null)
                throw new IllegalArgumentException("chain_damage property must have a 'damage_modifier' field");
            if (damageModifierEl.isJsonPrimitive() && damageModifierEl.getAsJsonPrimitive().isNumber())
            {
                final float multiplier = damageModifierEl.getAsFloat();
                damageModifier = (dmg, targetNum) -> dmg * (float)Math.pow(multiplier, targetNum);
            }
            else if (damageModifierEl.isJsonPrimitive() && damageModifierEl.getAsJsonPrimitive().isString())
            {
                final Expression exp = new Expression(damageModifierEl.getAsString());
                damageModifier = (dmg, targetNum) -> exp.with("dmg", BigDecimal.valueOf(dmg)).with("n", BigDecimal.valueOf(targetNum)).eval().floatValue();
            }
            else
                throw new IllegalArgumentException("Invalid chain_damage damage_modifier: " + damageModifierEl);

            JsonElement rangeEl = jsonObj.get("range");
            if (rangeEl == null)
                throw new IllegalArgumentException("chain_damage property must have a 'range' field");
            Function<Float, Float> rangeFunction = compileFormula(rangeEl, "dmg");

            return new ChainDamageOnHit(maxTargets, damageModifier, rangeFunction);
        });
        RPGDamageOverhaulAPI.registerPropertyBuilder("heal_on_hit", (element) -> {
            if (!element.isJsonPrimitive())
                throw new IllegalArgumentException("heal property must be a number or string");
            
            Function<Float, Float> healFunc = compileFormula(element, "dmg");
            return new HealOnHit(healFunc);
        });
        RPGDamageOverhaulAPI.registerPropertyBuilder("stacking", (element) -> {
            if (!element.isJsonObject())
                throw new IllegalArgumentException("stacking property must be a JSON object");

            var jsonObj = element.getAsJsonObject();

            JsonElement maxStacksEl = jsonObj.get("max_stacks");
            if (maxStacksEl == null || !maxStacksEl.isJsonPrimitive() || !maxStacksEl.getAsJsonPrimitive().isNumber())
                throw new IllegalArgumentException("stacking property must have a numeric 'max_stacks' field");
            int maxStacks = maxStacksEl.getAsInt();

            JsonElement durationEl = jsonObj.get("stack_duration");
            if (durationEl == null || !durationEl.isJsonPrimitive() || !durationEl.getAsJsonPrimitive().isNumber())
                throw new IllegalArgumentException("stacking property must have a numeric 'stack_duration' field");
            long stackDuration = durationEl.getAsLong();

            JsonElement damageFormulaEl = jsonObj.get("stack_damage");
            if (damageFormulaEl == null)
                throw new IllegalArgumentException("stacking property must have a 'stack_damage' field");
            
            Function<Float, Float> damageFormula = compileFormula(damageFormulaEl, "dmg");
            return new StackingOnHit(maxStacks, stackDuration, damageFormula);
        });
        RPGDamageOverhaulAPI.registerPropertyBuilder("modify_damage", (element) -> {
            if (!element.isJsonObject())
                throw new IllegalArgumentException("modify_damage property must be a JSON object");

            var jsonObj = element.getAsJsonObject();

            JsonElement modifierEl = jsonObj.get("modifier");
            if (modifierEl == null)
                throw new IllegalArgumentException("modify_damage property must have a 'modifier' field");
            Function<Float, Float> modifier = compileFormula(modifierEl, "dmg");

            JsonElement durationEl = jsonObj.get("duration");
            if (durationEl == null)
                throw new IllegalArgumentException("modify_damage property must have a 'duration' field");

            Function<Float, Float> durationFunc = compileFormula(durationEl, "dmg");

            JsonElement exceptionsEl = jsonObj.get("exceptions");
            Set<String> exceptions = new HashSet<>();
            boolean isWhitelist = false;
            if (exceptionsEl == null && jsonObj.has("whitelist"))
            {
                isWhitelist = true;
                exceptionsEl = jsonObj.get("whitelist");
            }
            if (exceptionsEl != null)
            {
                if (!exceptionsEl.isJsonArray())
                    throw new IllegalArgumentException("modify_damage exceptions field must be an array");
                for (var exceptEl : exceptionsEl.getAsJsonArray())                {
                    if (!exceptEl.isJsonPrimitive() || !exceptEl.getAsJsonPrimitive().isString())
                        throw new IllegalArgumentException("modify_damage exceptions must be an array of strings");
                    exceptions.add(exceptEl.getAsString());
                }
            }

            return new ModifyDamageOnHit(modifier, durationFunc, exceptions, isWhitelist);
        });
        RPGDamageOverhaulAPI.registerPropertyBuilder("apply_potion", (element) -> {
            Map<Holder<MobEffect>, Pair<Function<Float, Float>, Function<Float, Float>>> map = new HashMap<>();

            var obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet())
            {
                var effect = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(entry.getKey()));
                if (effect.isEmpty())
                    throw new IllegalArgumentException("Unknown potion effect: " + entry.getKey());
                var durationFunc = entry.getValue().getAsJsonObject().get("duration");
                if (durationFunc == null)
                    throw new IllegalArgumentException("Potion effect " + entry.getKey() + " must have a duration field");

                Function<Float, Float> durationFunction = compileFormula(durationFunc, "dmg");

                var amplifierEl = entry.getValue().getAsJsonObject().get("amplifier");
                if (amplifierEl == null)
                    throw new IllegalArgumentException("Potion effect " + entry.getKey() + " must have an amplifier field");

                Function<Float, Float> amplifierFunction = compileFormula(amplifierEl, "dmg");
                map.put(effect.get(), new Pair<>(durationFunction, amplifierFunction));
            }

            return new ApplyPotionOnHit(map);
        });

        RPGDamageOverhaulAPI.registerPropertyBuilder("attribute_modifiers", (element) -> {
            List<AttributeModifierOnHit.AttributeModifierData> modifiers = new ArrayList<>();

            var arr = element.getAsJsonArray();
            for (var modEl : arr)
            {
                var modObj = modEl.getAsJsonObject();
                var attrName = modObj.get("attribute").getAsString();
                var attr = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(attrName));
                if (attr == null || attr.isEmpty())
                    throw new IllegalArgumentException("Unknown attribute: " + attrName);
                var replaceTypeEl = modObj.get("replace_type");
                AttributeModifierOnHit.ReplaceType replaceType = AttributeModifierOnHit.ReplaceType.ALWAYS;
                if (replaceTypeEl != null)
                {
                    try {
                        replaceType = AttributeModifierOnHit.ReplaceType.valueOf(replaceTypeEl.getAsString().toUpperCase());
                    }
                    catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid attribute modifier replace_type: " + replaceTypeEl.getAsString());
                    }
                }
                var amountEl = modObj.get("amount");
                if (amountEl == null)
                    throw new IllegalArgumentException("Attribute modifier must have an 'amount' field for attribute " + attrName);

                Function<Float, Float> amountFunction = compileFormula(amountEl, "dmg");

                var durationEl = modObj.get("duration");
                if (durationEl == null)
                    throw new IllegalArgumentException("Attribute modifier must have a 'duration' field for attribute " + attrName);

                Function<Float, Float> durationFunction = compileFormula(durationEl, "dmg");

                var operationEl = modObj.get("operation");
                if (operationEl == null)
                    throw new IllegalArgumentException("Attribute modifier must have an 'operation' field for attribute " + attrName);
                AttributeModifier.Operation operation;
                try {
                    operation = AttributeModifier.Operation.valueOf(operationEl.getAsString().toUpperCase());
                }
                catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid attribute modifier operation for attribute " + attrName + ": " + operationEl.getAsString());
                }
                modifiers.add(new AttributeModifierOnHit.AttributeModifierData(attr.get(), replaceType, amountFunction, durationFunction, operation));
            }

            return new AttributeModifierOnHit(modifiers);
        });
        RPGDamageOverhaulAPI.registerPropertyAlias("attribute_modifiers", "attribute_modifier");
        RPGDamageOverhaulAPI.registerPropertyBuilder("set_wet", (element) -> {
            Function<Float, Float> durationFunc = compileFormula(element, "dmg");
            return new SetWetOnHit(durationFunc);
        });

        RPGDamageOverhaulAPI.registerPropertyBuilder("rename_attributes", (element) -> {
            if (!element.isJsonObject())
                throw new IllegalArgumentException("color_attributes property must be a JSON object");
            Map<ResourceLocation, DamageClass.DCAttribute> attributes = new HashMap<>();
            for (var attrEl : element.getAsJsonObject().entrySet())
            {
                var attrId = attrEl.getKey();
                DamageClass.DCAttribute dcAttr = DamageClass.DCAttribute.valueOf(attrEl.getValue().getAsString().toUpperCase());
                attributes.put(ResourceLocation.parse(attrId), dcAttr);
            }
            return new RenameAttributesProperty(attributes);
        });
        RPGDamageOverhaulAPI.registerPropertyBuilder(PARENT_DEFENSE_EFFECTIVENESS_PROPERTY, (element) -> new SimpleProperty<>(PARENT_DEFENSE_EFFECTIVENESS_PROPERTY, element.getAsFloat()));
        RPGDamageOverhaulAPI.registerPropertyBuilder(PARENT_DAMAGE_DEFENSE_EFFECTIVENESS_PROPERTY, (element) -> new SimpleProperty<>(PARENT_DAMAGE_DEFENSE_EFFECTIVENESS_PROPERTY, element.getAsFloat()));
    }

    public static void onDcRegister(DamageClass dc)
    {
        Services.PLATFORM.fireDamageClassRegisteredEvent(dc);
    }
}
