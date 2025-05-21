package com.httpedor.rpgdamageoverhaul;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.DamageHandler;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.httpedor.rpgdamageoverhaul.events.DamageClassRegisteredEvent;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import s_com.udojava.evalex.Expression;

import java.math.BigDecimal;
import java.util.*;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(RPGDamageOverhaul.MODID)
public class RPGDamageOverhaul {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "rpgdamageoverhaul";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    public static DatapackLoader dl = new DatapackLoader();


    public static final Map<Enchantment, Tuple<DamageClass, Float>> damageEnchantments = new HashMap<>();
    public static final Map<LivingEntity, Tuple<Float, Long>> noHealingUntil = new HashMap<>();
    public static final Map<LivingEntity, Map<String, List<Tuple<Float, Long>>>> dmgStacks = new HashMap<>();
    public static final Map<LivingEntity, Tuple<Float, Long>> increasedDamage = new HashMap<>();
    public static final Set<DamageClass> increasedDamageExceptions = new HashSet<>();
    public static final Map<UUID, Attribute> transientModifiers = new HashMap<>();
    public static final Map<LivingEntity, Map<UUID, Long>> transientModifiersDuration = new HashMap<>();

    public static final Map<ResourceLocation, List<ResourceLocation>> mappedDamageTypes = new HashMap<>();
    public static final Map<ResourceLocation, List<ResourceLocation>> mappedTags = new HashMap<>();

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "main"),
            () -> "1.0",
            s -> true,
            s -> true
    );

    private void spawnHitParticles(ServerLevel world, ParticleOptions parameters, double x, double y, double z, int amount)
    {
        double maxSpeed  = 0.1;
        for (int i = 0; i < amount; i++)
        {
            world.sendParticles(parameters, x, y, z, amount, 0d, 0d, 0d, maxSpeed);
        }
    }


    public RPGDamageOverhaul() {
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        CHANNEL.messageBuilder(HashMap.class, 0, NetworkDirection.LOGIN_TO_CLIENT)
                .decoder((buf) -> {
                    HashMap<String, JsonObject> entries = new HashMap<>();
                    buf.readMap(i -> entries, FriendlyByteBuf::readUtf, (res) -> JsonParser.parseString(res.readUtf()).getAsJsonObject());
                    return entries;
                })
                .encoder((map, buf) -> {
                    HashMap<String, JsonObject> entries = (HashMap<String, JsonObject>) map;
                    buf.writeMap(entries, FriendlyByteBuf::writeUtf, (buf1, el) -> buf1.writeUtf(el.toString()));
                }).consumerMainThread((map, ctx) -> {
                    for (Object o: map.entrySet())
                    {
                        Map.Entry<String, JsonObject> entry = (Map.Entry<String, JsonObject>) o;
                        dl.registerDamageClass(entry.getKey(), entry.getValue(), null);
                    }
                }).noResponse().buildLoginPacketList((isLocal) -> List.of(Pair.of("rpgdologinpacket", dl.dcEntries))).add();

        registerOnHitEffects();

    }

    @SubscribeEvent
    public void onDCRegistered(DamageClassRegisteredEvent e)
    {
        var dc = e.getDamageClass();

        // Register potion attributes
        if (dc.properties.containsKey("potions"))
        {
            JsonObject potions = dc.properties.get("potions").getAsJsonObject();
            for (Map.Entry<String, JsonElement> attribute: potions.entrySet())
            {
                Attribute attr = switch (attribute.getKey()) {
                    case "resistance" -> dc.resistanceAttribute;
                    case "damage" -> dc.dmgAttribute;
                    case "armor" -> dc.armorAttribute;
                    case "absorption" -> dc.absorptionAttribute;
                    default -> null;
                };
                if (attr == null)
                {
                    RPGDamageOverhaul.LOGGER.warn("Unknown attribute: {} for damage class potions: {}", attribute.getKey(), dc.name);
                    continue;
                }
                JsonObject potionAttrs = attribute.getValue().getAsJsonObject();
                for (Map.Entry<String, JsonElement> potion: potionAttrs.entrySet())
                {
                    MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(potion.getKey()));
                    if (effect == null)
                    {
                        RPGDamageOverhaul.LOGGER.warn("Unknown potion effect: {} for damage class potions: {}", potion.getKey(), dc.name);
                        continue;
                    }
                    double value = potion.getValue().getAsDouble();
                    effect.addAttributeModifier(attr, UUID.randomUUID().toString(), value, AttributeModifier.Operation.ADDITION);
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
                    var enchantment = ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(enchant.getKey()));
                    if (enchantment == null)
                    {
                        RPGDamageOverhaul.LOGGER.warn("Unknown enchantment: {} for damage class : {}", enchant.getKey(), dc.name);
                        continue;
                    }
                    float value = enchant.getValue().getAsFloat();
                    damageEnchantments.put(enchantment, new Tuple<>(dc, value));
                }
            }
        }

        //Register DT aliases
        if (dc.properties.containsKey("damageTypes"))
        {
            var dts = dc.properties.get("damageTypes").getAsJsonArray().asList();
            if (!mappedDamageTypes.containsKey(dc.damageTypeKey.location()))
                mappedDamageTypes.put(dc.damageTypeKey.location(), new ArrayList<>());
            for (var damageTypeEl : dts)
            {
                mappedDamageTypes.get(dc.damageTypeKey.location()).add(new ResourceLocation(damageTypeEl.getAsString()));
            }
        }

        //Register DC DT Tags
        if (dc.properties.containsKey("tags"))
        {
            var tags = dc.properties.get("tags").getAsJsonArray().asList();
            if (!mappedTags.containsKey(dc.damageTypeKey.location()))
                mappedTags.put(dc.damageTypeKey.location(), new ArrayList<>());
            for (var tag : tags)
            {
                mappedTags.get(dc.damageTypeKey.location()).add(new ResourceLocation(tag.getAsString()));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDatapackReload(AddReloadListenerEvent e)
    {
        dl.ra = e.getRegistryAccess();
        e.addListener(dl);
    }

    private void registerOnHitEffects()
    {
        RPGDamageOverhaulAPI.registerOnHitEffect(new ResourceLocation("rpgdamageoverhaul", "particles"), (target, source, dmg) -> {
            DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.type());
            var particleEl = dc.properties.getOrDefault("particle", null);
            if (particleEl == null)
                return;

            String particleId = particleEl.getAsString();
            if (!target.level().isClientSide)
            {
                SimpleParticleType pt = (SimpleParticleType) ForgeRegistries.PARTICLE_TYPES.getValue(new ResourceLocation(particleId));
                if (pt == null)
                {
                    System.out.println("Particle not found: " + particleId);
                    return;
                }

                try {
                    spawnHitParticles((ServerLevel) target.level(), pt.getDeserializer().fromCommand(pt, new StringReader("")), target.getX(), target.getEyeY(), target.getZ(), (int) (dmg/2)+1);
                } catch (CommandSyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        RPGDamageOverhaulAPI.registerOnHitEffect(new ResourceLocation("rpgdamageoverhaul", "set_fire"), (target, source, dmg) -> {
            if (source.getEntity() != null)
                target.setSecondsOnFire((int) Math.round(dmg/2));
            target.setTicksFrozen(0);
        });
        RPGDamageOverhaulAPI.registerOnHitEffect(new ResourceLocation("rpgdamageoverhaul", "set_frozen"), (target, source, dmg) -> {
            target.setTicksFrozen((int) Math.round(dmg/1.5 * 20));
            target.setRemainingFireTicks(0);
        });
        RPGDamageOverhaulAPI.registerOnHitEffect(new ResourceLocation("rpgdamageoverhaul", "anti_heal"), (target, source, dmg) -> {
            float percent;
            long time;
            DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.type());
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
            if (noHealingUntil.containsKey(target) && noHealingUntil.get(target).getA() > percent)
                return;
            noHealingUntil.put(target, new Tuple<>(percent, System.currentTimeMillis() + time));
        });
        RPGDamageOverhaulAPI.registerOnHitEffect(new ResourceLocation("rpgdamageoverhaul", "chain_lightning"), (target, source, dmg) -> {
            DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.type());
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
                current = current.level().getEntitiesOfClass(LivingEntity.class, AABB.ofSize(current.position(), range, range, range), (e) -> !traveled.contains(e)).stream().findFirst().orElse(null);
                if (current == null)
                    break;
                current.hurt(dc.createDamageSource(source.getEntity(), source.getDirectEntity(), false), (float) currentDmg);
                if (dc.onHitEffects.contains(new ResourceLocation("rpgdamageoverhaul", "particles")))
                    DamageHandler.executeOnHitEffect(new ResourceLocation("rpgdamageoverhaul", "particles"), current, source, (float) currentDmg);
                currentDmg *= damagePercentage;
                targets++;
            }
        });
        RPGDamageOverhaulAPI.registerOnHitEffect(new ResourceLocation("rpgdamageoverhaul", "heal"), (target, source, dmg) -> {
            DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.type());

            if (source.getEntity() instanceof LivingEntity le)
            {
                var multiplierEl = dc.properties.getOrDefault("heal", null);
                double multiplier;
                if (multiplierEl == null)
                    multiplier = 1;
                else if (multiplierEl.getAsJsonPrimitive().isNumber())
                    multiplier = multiplierEl.getAsDouble();
                else
                {
                    Expression exp = new Expression(multiplierEl.getAsString()).with("dmg", BigDecimal.valueOf(dmg));
                    multiplier = exp.eval().doubleValue();
                }
                le.heal((float) (dmg * multiplier));
            }
        });
        RPGDamageOverhaulAPI.registerOnHitEffect(new ResourceLocation("rpgdamageoverhaul", "stacking"), (target, source, dmg) -> {
            DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.type());
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
            stacks.removeIf(stack -> stack.getB() + stacksDuration < System.currentTimeMillis());
            if (stacks.size() >= maxStacks)
                stacks.remove(0);

            var currentDmg = stacks.stream().mapToDouble(Tuple::getA).sum();
            target.hurt(dc.createDamageSource(source.getEntity(), source.getDirectEntity(), false), ((float) currentDmg));

            if (obj.has("formula"))
            {
                Expression exp = new Expression(obj.get("formula").getAsString()).with("dmg", BigDecimal.valueOf(dmg));
                stacks.add(new Tuple<>(exp.eval().floatValue(), System.currentTimeMillis()));
            }
            else
                stacks.add(new Tuple<>(dmg.floatValue()/3, System.currentTimeMillis()));
        });
        RPGDamageOverhaulAPI.registerOnHitEffect(new ResourceLocation("rpgdamageoverhaul", "increase_damage"), (target, source, dmg) -> {
            DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.type());
            var obj = dc.properties.get("increaseDamage").getAsJsonObject();
            float duration;
            float dmgIncrease;
            if (obj.has("except"))
            {
                var except = obj.get("except").getAsJsonArray();
                for (var el : except)
                {
                    var tdc = RPGDamageOverhaulAPI.getDamageClass(el.getAsString());
                    if (tdc != null)
                        increasedDamageExceptions.add(tdc);
                }
            }
            if (obj.has("duration"))
            {
                var el = obj.get("duration");
                if (el.getAsJsonPrimitive().isNumber())
                    duration = obj.get("duration").getAsFloat();
                else
                {
                    Expression exp = new Expression(el.getAsString()).with("dmg", BigDecimal.valueOf(dmg));
                    duration = exp.eval().floatValue();
                }
            }
            else
                duration = 5;
            if (obj.has("multiplier"))
            {
                var el = obj.get("multiplier");
                if (el.getAsJsonPrimitive().isNumber())
                    dmgIncrease = obj.get("multiplier").getAsFloat();
                else
                {
                    Expression exp = new Expression(el.getAsString()).with("dmg", BigDecimal.valueOf(dmg));
                    dmgIncrease = exp.eval().floatValue();
                }
            }
            else
                dmgIncrease = (float) (1 + 0.03f * dmg);
            increasedDamage.put(target, new Tuple<>(dmgIncrease, System.currentTimeMillis() + (long) (duration * 1000)));
        });
        RPGDamageOverhaulAPI.registerOnHitEffect(new ResourceLocation("rpgdamageoverhaul", "apply_potion"), (target, source, dmg) -> {
            DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.type());

            var obj = dc.properties.get("applyPotion").getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet())
            {
                var effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(entry.getKey()));
                if (effect == null)
                {
                    LOGGER.warn("Unknown potion effect: {} for damage class applyPotion: {}", entry.getKey(), dc.name);
                    continue;
                }
                var duration = entry.getValue().getAsFloat();
                target.addEffect(new MobEffectInstance(effect, (int) (duration * 20), 0));
            }
        });
        RPGDamageOverhaulAPI.registerOnHitEffect(new ResourceLocation("rpgdamageoverhaul", "attribute_modifier"), (target, source, dmg) -> {
            DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.type());
            var obj = dc.properties.get("attributeModifier").getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet())
            {
                var name = entry.getKey();
                var modifier = entry.getValue().getAsJsonObject();
                var attribute = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(modifier.get("attribute").getAsString()));
                if (attribute == null)
                {
                    LOGGER.warn("Unknown attribute: {} for damage class attributeModifier: {}", modifier.get("attribute").getAsString(), dc.name);
                    continue;
                }
                if (target.getAttribute(attribute) == null)
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

                var mod = new AttributeModifier(id, name, amount, AttributeModifier.Operation.valueOf(operation));
                transientModifiersDuration.computeIfAbsent(target, (t) -> new HashMap<>()).put(id, System.currentTimeMillis() + Math.round(duration * 1000L));
                if (target.getAttribute(attribute).getModifier(mod.getId()) != null)
                {
                    double currentAmount = target.getAttribute(attribute).getModifier(mod.getId()).getAmount();
                    if (replaceType.equalsIgnoreCase("always")
                    || (replaceType.equalsIgnoreCase("lower") && amount < currentAmount)
                    || (replaceType.equalsIgnoreCase("higher") && amount > currentAmount))
                    {
                        target.getAttribute(attribute).removeModifier(mod.getId());
                    }
                    else
                    {
                        return;
                    }
                }
                double healthRatio = target.getHealth() / target.getMaxHealth();
                target.getAttribute(attribute).addTransientModifier(mod);
                transientModifiers.put(id, attribute);
                if (attribute == Attributes.MAX_HEALTH)
                {
                    target.setHealth((float) (target.getMaxHealth() * healthRatio));
                }
            }
        });
    }
}
