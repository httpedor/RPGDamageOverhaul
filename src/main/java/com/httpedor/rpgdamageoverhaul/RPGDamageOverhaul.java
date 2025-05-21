package com.httpedor.rpgdamageoverhaul;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.DamageHandler;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.httpedor.rpgdamageoverhaul.events.DamageClassRegisteredCallback;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Box;
import org.slf4j.Logger;
import s_com.udojava.evalex.Expression;

import java.math.BigDecimal;
import java.util.*;

public class RPGDamageOverhaul implements ModInitializer {
    public static final Map<Enchantment, Pair<DamageClass, Float>> damageEnchantments = new HashMap<>();
    public static final Map<LivingEntity, Pair<Float, Long>> noHealingUntil = new HashMap<>();
    public static final Map<LivingEntity, Map<String, List<Pair<Float, Long>>>> dmgStacks = new HashMap<>();
    public static final Map<LivingEntity, Pair<Float, Long>> increasedDamage = new HashMap<>();
    public static final Set<DamageClass> increasedDamageExceptions = new HashSet<>();
    public static final Map<UUID, EntityAttribute> transientModifiers = new HashMap<>();
    public static final Map<LivingEntity, Map<UUID, Long>> transientModifiersDuration = new HashMap<>();

    public static final Map<Identifier, List<Identifier>> mappedDamageTypes = new HashMap<>();
    public static final Map<Identifier, List<Identifier>> mappedTags = new HashMap<>();

    public static final Logger LOGGER = LogUtils.getLogger();

    private void spawnHitParticles(ServerWorld world, ParticleEffect parameters, double x, double y, double z, int amount)
    {
        double maxSpeed  = 0.1;
        for (int i = 0; i < amount; i++)
        {
            world.spawnParticles(parameters, x, y, z, amount, 0d, 0d, 0d, maxSpeed);
        }
    }

    @Override
    public void onInitialize() {

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ServerPlayerEntity spe)
            {
                Registry<DamageType> reg = world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE);
                PacketByteBuf buf = PacketByteBufs.create();
                var dtTypes = RPGDamageOverhaulAPI.getRPGDamageTypes();
                buf.writeInt(dtTypes.size());
                for (String dt : dtTypes)
                {
                    buf.writeString(dt);
                    buf.writeInt(reg.getRawId(reg.get(RegistryKey.of(RegistryKeys.DAMAGE_TYPE, new Identifier("rpgdamageoverhaul", dt)))));
                }
                ServerPlayNetworking.send(spe, Identifier.of("rpgdamageoverhaul", "damage_type"), buf);
            }
        });

        DamageClassRegisteredCallback.EVENT.register((dc) -> {
            // Register potion attributes
            if (dc.properties.containsKey("potions"))
            {
                JsonObject potions = dc.properties.get("potions").getAsJsonObject();
                for (Map.Entry<String, JsonElement> attribute: potions.entrySet())
                {
                    EntityAttribute attr = switch (attribute.getKey()) {
                        case "resistance" -> dc.resistanceAttribute;
                        case "damage" -> dc.dmgAttribute;
                        case "armor" -> dc.armorAttribute;
                        case "absorption" -> dc.absorptionAttribute;
                        default -> null;
                    };
                    if (attr == null)
                    {
                        LOGGER.warn("Unknown attribute: {} for damage class potions: {}", attribute.getKey(), dc.name);
                        continue;
                    }
                    JsonObject potionAttrs = attribute.getValue().getAsJsonObject();
                    for (Map.Entry<String, JsonElement> potion: potionAttrs.entrySet())
                    {
                        StatusEffect effect = Registries.STATUS_EFFECT.get(new Identifier(potion.getKey()));
                        if (effect == null)
                        {
                            LOGGER.warn("Unknown potion effect: {} for damage class potions: {}", potion.getKey(), dc.name);
                            continue;
                        }
                        double value = potion.getValue().getAsDouble();
                        effect.addAttributeModifier(attr, UUID.randomUUID().toString(), value, EntityAttributeModifier.Operation.ADDITION);
                    }
                }
            }

            //Register enchantments
            if (dc.properties.containsKey("enchantments"))
            {
                JsonObject enchantments = dc.properties.get("enchantments").getAsJsonObject();
                if (enchantments.has("damage"))
                {
                    JsonObject dmgEnchants = enchantments.getAsJsonObject("damage");
                    for (Map.Entry<String, JsonElement> enchant: dmgEnchants.entrySet())
                    {
                        var enchantment = Registries.ENCHANTMENT.get(new Identifier(enchant.getKey()));
                        if (enchantment == null)
                        {
                            RPGDamageOverhaul.LOGGER.warn("Unknown enchantment: {} for damage class : {}", enchant.getKey(), dc.name);
                            continue;
                        }
                        float value = enchant.getValue().getAsFloat();
                        damageEnchantments.put(enchantment, new Pair<>(dc, value));
                    }
                }
            }

            //Register DT aliases
            if (dc.properties.containsKey("damageTypes"))
            {
                var dts = dc.properties.get("damageTypes").getAsJsonArray().asList();
                if (!mappedDamageTypes.containsKey(dc.damageType.getValue()))
                    mappedDamageTypes.put(dc.damageType.getValue(), new ArrayList<>());
                for (var damageTypeEl : dts)
                {
                    mappedDamageTypes.get(dc.damageType.getValue()).add(new Identifier(damageTypeEl.getAsString()));
                }
            }

            //Register DC DT Tags
            if (dc.properties.containsKey("tags"))
            {
                var tags = dc.properties.get("tags").getAsJsonArray().asList();
                if (!mappedTags.containsKey(dc.damageType.getValue()))
                    mappedTags.put(dc.damageType.getValue(), new ArrayList<>());
                for (var tag : tags)
                {
                    mappedTags.get(dc.damageType.getValue()).add(new Identifier(tag.getAsString()));
                }
            }
        });


        RPGDamageOverhaulAPI.registerOnHitEffect(new Identifier("rpgdamageoverhaul", "particles"), (target, source, dmg) -> {
            DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.getType());
            var particleEl = dc.properties.getOrDefault("particle", null);
            if (particleEl == null)
                return;

            String particleId = particleEl.getAsString();
            if (!target.getWorld().isClient)
            {
                DefaultParticleType pt = (DefaultParticleType) Registries.PARTICLE_TYPE.get(new Identifier(particleId));
                if (pt == null)
                    System.out.println("Particle not found: " + particleId);

                try {
                    spawnHitParticles((ServerWorld) target.getWorld(), pt.getParametersFactory().read(pt, new StringReader("")), target.getX(), target.getEyeY(), target.getZ(), (int) (dmg/2)+1);
                } catch (CommandSyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        RPGDamageOverhaulAPI.registerOnHitEffect(new Identifier("rpgdamageoverhaul", "set_fire"), (target, source, dmg) -> {
            target.setOnFireFor((int) Math.round(dmg/2));
            target.setFrozenTicks(0);
        });
        RPGDamageOverhaulAPI.registerOnHitEffect(new Identifier("rpgdamageoverhaul", "set_frozen"), (target, source, dmg) -> {
            target.setFrozenTicks((int) Math.round(dmg/1.5 * 20));
            target.setFireTicks(0);
        });
        RPGDamageOverhaulAPI.registerOnHitEffect(new Identifier("rpgdamageoverhaul", "anti_heal"), (target, source, dmg) -> {
            float percent;
            long time;
            DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.getType());
            if (dc.properties.containsKey("antiHeal"))
            {
                var element = dc.properties.get("antiHeal").getAsJsonObject();
                percent = (float) (element.get("percentBlockedPerHP").getAsDouble() * dmg);
                time = (long) (element.get("durationPerHeart").getAsFloat() * 1000) * (long) (dmg/2);
            }
            else
            {
                percent = 0.5f;
                time = 500 * (long) (dmg/2);
            }
            percent = Math.min(percent, 1);
            noHealingUntil.put(target, new Pair<>(percent, System.currentTimeMillis() + time));
        });
        RPGDamageOverhaulAPI.registerOnHitEffect(new Identifier("rpgdamageoverhaul", "chain_lightning"), (target, source, dmg) -> {
            DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.getType());
            var obj = dc.properties.get("chainLightning").getAsJsonObject();
            double range = obj.get("range").getAsDouble();
            int maxTargets = obj.get("maxTargets").getAsInt();
            double damagePercentage = obj.get("damagePercentage").getAsDouble();

            int targets = 0;
            double currentDmg = dmg * damagePercentage;
            Set<LivingEntity> traveled = new HashSet<>();
            LivingEntity current = target;
            while (current != null && targets < maxTargets)
            {
                traveled.add(current);
                current = current.getWorld().getEntitiesByClass(LivingEntity.class, Box.of(current.getPos(), range, range, range), (e) -> !traveled.contains(e)).stream().findFirst().orElse(null);
                if (current == null)
                    break;
                current.damage(dc.createDamageSource(source.getAttacker(), source.getSource(), false), (float) currentDmg);
                if (dc.onHitEffects.contains(new Identifier("rpgdamageoverhaul", "particles")))
                    DamageHandler.executeOnHitEffect(new Identifier("rpgdamageoverhaul", "particles"), current, source, (float) currentDmg);
                currentDmg *= damagePercentage;
                targets++;
            }
        });
        RPGDamageOverhaulAPI.registerOnHitEffect(new Identifier("rpgdamageoverhaul", "heal"), (target, source, dmg) -> {
            DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.getType());

            if (source.getAttacker() instanceof LivingEntity le)
            {
                var multiplierEl = dc.properties.getOrDefault("healMultiplier", null);
                double multiplier = multiplierEl == null ? 1 : multiplierEl.getAsDouble();
                le.heal((float) (dmg * multiplier));
            }
        });
        RPGDamageOverhaulAPI.registerOnHitEffect(new Identifier("rpgdamageoverhaul", "stacking"), (target, source, dmg) -> {
            DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.getType());
            var obj = dc.properties.get("stacking").getAsJsonObject();
            String stackName;
            if (obj.has("stackName"))
                stackName = obj.get("stackName").getAsString();
            else
                stackName = dc.name;
            var stacks = dmgStacks.computeIfAbsent(target, (t) -> new HashMap<>()).computeIfAbsent(stackName, (s) -> new ArrayList<>());
            int maxStacks;
            long stacksDuration;
            if (obj.has("maxStacks"))
                maxStacks = obj.get("maxStacks").getAsInt();
            else
                maxStacks = 5;
            if (obj.has("stacksDuration"))
                stacksDuration = (long) (obj.get("stacksDuration").getAsFloat() * 1000);
            else
                stacksDuration = 5000;
            stacks.removeIf(stack -> stack.getRight() + stacksDuration < System.currentTimeMillis());
            if (stacks.size() >= maxStacks)
                stacks.remove(0);

            var currentDmg = stacks.stream().mapToDouble(Pair::getLeft).sum();
            target.damage(dc.createDamageSource(source.getAttacker(), source.getSource(), false), ((float) currentDmg));

            if (obj.has("stackDmgMultiplier"))
                stacks.add(new Pair<>(dmg.floatValue() * obj.get("stackDmgMultiplier").getAsFloat(), System.currentTimeMillis()));
            else
                stacks.add(new Pair<>(dmg.floatValue()/4, System.currentTimeMillis()));
        });
        RPGDamageOverhaulAPI.registerOnHitEffect(new Identifier("rpgdamageoverhaul", "increase_damage"), (target, source, dmg) -> {
            DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.getType());
            var obj = dc.properties.get("increaseDamage").getAsJsonObject();
            float duration;
            float multiplierPerHP;
            if (obj.has("duration"))
                duration = obj.get("duration").getAsFloat();
            else
                duration = 5;
            if (obj.has("multiplierPerHP"))
                multiplierPerHP = obj.get("multiplierPerHP").getAsFloat();
            else
                multiplierPerHP = 0.03f;
            increasedDamage.put(target, new Pair<>((float)(multiplierPerHP * dmg), System.currentTimeMillis() + (long) (duration * 1000)));
        });
        RPGDamageOverhaulAPI.registerOnHitEffect(new Identifier("rpgdamageoverhaul", "apply_potion"), (target, source, dmg) -> {
            DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.getType());

            var obj = dc.properties.get("applyPotion").getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet())
            {
                var effect = Registries.STATUS_EFFECT.get(new Identifier(entry.getKey()));
                if (effect == null)
                {
                    LOGGER.warn("Unknown potion effect: {} for damage class applyPotion: {}", entry.getKey(), dc.name);
                    continue;
                }
                var duration = entry.getValue().getAsFloat();
                target.addStatusEffect(new StatusEffectInstance(effect, (int) (duration * 20), 0));
            }
        });
        RPGDamageOverhaulAPI.registerOnHitEffect(new Identifier("rpgdamageoverhaul", "attribute_modifier"), (target, source, dmg) -> {
            DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.getType());
            var obj = dc.properties.get("attributeModifier").getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet())
            {
                var name = entry.getKey();
                var modifier = entry.getValue().getAsJsonObject();
                var attribute = Registries.ATTRIBUTE.get(new Identifier(modifier.get("attribute").getAsString()));
                if (attribute == null)
                {
                    LOGGER.warn("Unknown attribute: {} for damage class attributeModifier: {}", modifier.get("attribute").getAsString(), dc.name);
                    continue;
                }
                if (target.getAttributeInstance(attribute) == null)
                {
                    continue;
                }
                var operation = modifier.get("operation").getAsString();
                double amount;
                if (modifier.get("amount").getAsJsonPrimitive().isNumber())
                    amount = modifier.get("amount").getAsDouble();
                else
                {
                    Expression exp = new Expression(modifier.get("amount").getAsString()).with("dmg", BigDecimal.valueOf(dmg));
                    amount = exp.eval().doubleValue();
                }
                UUID id;
                if (modifier.has("id"))
                    id = UUID.fromString(modifier.get("id").getAsString());
                else
                    id = UUID.randomUUID();
                double duration;
                if (modifier.get("duration").getAsJsonPrimitive().isNumber())
                    duration = modifier.get("duration").getAsDouble();
                else
                {
                    Expression exp = new Expression(modifier.get("duration").getAsString()).with("dmg", BigDecimal.valueOf(dmg));
                    duration = exp.eval().doubleValue();
                }

                String replaceType = "ALWAYS";
                if (modifier.has("replaceType"))
                    replaceType = modifier.get("replaceType").getAsString();

                var mod = new EntityAttributeModifier(id, name, amount, EntityAttributeModifier.Operation.valueOf(operation));
                transientModifiersDuration.computeIfAbsent(target, (t) -> new HashMap<>()).put(id, System.currentTimeMillis() + Math.round(duration * 1000L));
                if (target.getAttributeInstance(attribute).getModifier(mod.getId()) != null)
                {
                    double currentAmount = target.getAttributeInstance(attribute).getModifier(mod.getId()).getValue();
                    if (replaceType.equalsIgnoreCase("always")
                            || (replaceType.equalsIgnoreCase("lower") && amount < currentAmount)
                            || (replaceType.equalsIgnoreCase("higher") && amount > currentAmount))
                    {
                        target.getAttributeInstance(attribute).removeModifier(mod.getId());
                    }
                    else
                    {
                        return;
                    }
                }
                double healthRatio = target.getHealth() / target.getMaxHealth();
                target.getAttributeInstance(attribute).addTemporaryModifier(mod);
                transientModifiers.put(id, attribute);
                if (attribute == EntityAttributes.GENERIC_MAX_HEALTH)
                {
                    target.setHealth((float) (target.getMaxHealth() * healthRatio));
                }
            }
        });

        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new ReloadListener());
    }
}
