package com.httpedor.rpgdamageoverhaul;

import java.text.DecimalFormat;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.httpedor.rpgdamageoverhaul.api.DamageClass.DCAttribute;
import com.httpedor.rpgdamageoverhaul.damageproperties.RenameAttributesProperty;
import com.httpedor.rpgdamageoverhaul.damageproperties.EnchantmentAttributesProperty;

import com.httpedor.rpgdamageoverhaul.damageproperties.ParentProperty;
import com.httpedor.rpgdamageoverhaul.damageproperties.onhit.ModifyDamageOnHit;
import com.httpedor.rpgdamageoverhaul.damageproperties.onhit.SetWetOnHit;
import net.minecraft.ChatFormatting;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class SharedLogic {
    /**
     * Gets how much of an attribute an entity has, including enchantments, attribute injections, etc.
     * @param dc The damage class
     * @param attribute The attribute to get
     * @param entity The entity to get the attribute of
     * @return The value of the attribute
     */
    public static double getAttributeValue(DamageClass dc, DCAttribute attribute, LivingEntity entity)
    {
        var registry = entity.level().registryAccess().registry(Registries.ENCHANTMENT).get();
        // Not getAttributeValue directly: that throws "Can't find attribute" for an entity whose AttributeMap was
        // built before this damage class' attributes were injected into the entity type's supplier (a LocalPlayer
        // that joined before the sync, or anything already loaded when a /reload adds a new damage class). Such an
        // entity simply has none of this attribute, so read it as 0.
        var attrHolder = dc.getAttribute(attribute);
        var attr = entity.getAttributes().hasAttribute(attrHolder) ? entity.getAttributeValue(attrHolder) : 0d;

        var enchantsProp = dc.getPropertyOfType(EnchantmentAttributesProperty.class);
        if (enchantsProp != null)
        {

            for (var entry : enchantsProp.getAttributeModifiers().entrySet())
            {
                var enchantmentId = entry.getKey();
                var enchantment = registry.getHolder(enchantmentId);
                var modifier = entry.getValue().get(attribute);
                if (modifier != null && enchantment.isPresent())
                {
                    var level = EnchantmentHelper.getEnchantmentLevel(enchantment.get(), entity);
                    attr += modifier.apply(level);
                }
            }
        }

        return attr;
    }
    public static double getAttributeRespectingRelatives(DamageClass dc, DCAttribute attribute, LivingEntity entity)
    {
        float defenseMultiplier = 1f;
        Float multProp = dc.getSimplePropertyById(DCProperties.PARENT_DEFENSE_EFFECTIVENESS_PROPERTY);
        if (multProp != null)
            defenseMultiplier = multProp;
        double value = getAttributeValue(dc, attribute, entity);
        var parent = dc.getParent();
        while (parent != null)
        {
            var parentValue = getAttributeValue(parent, attribute, entity);
            value += parentValue;

            multProp = parent.getSimplePropertyById(DCProperties.PARENT_DEFENSE_EFFECTIVENESS_PROPERTY);
            if (multProp != null)
            {
                if (parentValue < 0)
                    defenseMultiplier *= 1 + (1 - multProp);
                else
                    defenseMultiplier *= multProp;
            }

            parent = parent.getParent();
        }
        if (attribute != DCAttribute.DAMAGE)
            value *= defenseMultiplier;

        Map<String, Float> cachedMultipliers = new HashMap<>();
        Stack<DamageClass> stack = new Stack<>();
        stack.add(dc);
        while (!stack.isEmpty())
        {
            var current = stack.pop();
            stack.addAll(ParentProperty.getChildren(current));

            var currentAttr = getAttributeValue(current, attribute, entity);
            if (attribute != DCAttribute.DAMAGE)
            {
                float currentMult = current.getSimplePropertyById(DCProperties.PARENT_DAMAGE_DEFENSE_EFFECTIVENESS_PROPERTY, 1f);
                currentMult *= cachedMultipliers.getOrDefault(current.getParentId(), 1f);
                cachedMultipliers.put(current.name, currentMult);
                if (currentAttr < 0)
                    currentAttr *= 1 + (1 - currentMult);
                else
                    currentAttr *= currentMult;
            }

            value += currentAttr;
        }

        return value;
    }
    public static float getDamagePostResistance(float damage, LivingEntity entity, DamageType dt)
    {
        var dc = RPGDamageOverhaulAPI.getDamageClass(dt);
        if (dc == null)
            return damage;

        damage = (float)(damage * (1d - getAttributeRespectingRelatives(dc, DCAttribute.RESISTANCE, entity)));
        return damage;
    }
    public static float getDamagePostArmor(float damage, LivingEntity entity, DamageSource source)
    {
        var dc = RPGDamageOverhaulAPI.getDamageClass(source.type());
        if (dc == null)
            return damage;
        double armor = getAttributeRespectingRelatives(dc, DCAttribute.ARMOR, entity);
        double toughness = entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        BiFunction<Float, Float, Float> armorModifyProp = dc.getSimplePropertyById(DCProperties.ARMOR_MODIFIER_PROPERTY);

        if (armorModifyProp != null)
            armor = (double) armorModifyProp.apply(damage, (float)armor);

        return CombatRules.getDamageAfterAbsorb(entity, damage, source, (float)armor, (float)toughness);
    }

    public static float modifyDamagePreArmor(float damage, LivingEntity entity, DamageClass dc)
    {
        damage = ModifyDamageOnHit.applyDamageModifiers(entity, damage, dc);
        Function<Float, Float> modifier = dc.getSimplePropertyById(DCProperties.WATER_DAMAGE_PROPERTY);
        if (modifier != null && SetWetOnHit.isWet(entity))
            damage = modifier.apply(damage);

        return damage;
    }

    private static boolean shouldDebugTooltip(ItemStack is)
    {
        //return is.getItem() == Items.DIAMOND_CHESTPLATE || is.getItem() == Items.TRIDENT;
        return false;
    }

    private static String safeComponentString(Component c)
    {
        if (c == null)
            return "<null>";
        try {
            return c.getString();
        } catch (Exception ex) {
            return "<component toString failed: " + ex.getClass().getSimpleName() + ">";
        }
    }

    private static String safeArgsSummary(Object[] args)
    {
        if (args == null)
            return "null";
        StringBuilder sb = new StringBuilder("[");
        for (int j = 0; j < args.length; j++)
        {
            if (j > 0)
                sb.append(", ");
            Object a = args[j];
            if (a == null)
            {
                sb.append("null");
                continue;
            }
            sb.append(a.getClass().getSimpleName());
            if (a instanceof Component comp)
                sb.append("(\"").append(safeComponentString(comp)).append("\")");
            else
                sb.append("(").append(String.valueOf(a)).append(")");
        }
        sb.append("]");
        return sb.toString();
    }

    public static void modifyTooltip(List<Component> lines, ItemStack stack, boolean isAdvanced)
    {
        final boolean dbg = shouldDebugTooltip(stack);

        List<Component> dcLines = new ArrayList<>();
        int atkLineIndex = -1;
        int armorLineIndex = -1;
        boolean isArmor = false;

        if (dbg)
        {
            try {
                Constants.LOG.info("[TooltipDbg] START item={} hoverName=\"{}\" lines={} advanced={} filter=\"{}\"",
                        stack == null ? "<null>" : stack.getDescriptionId(),
                        stack == null ? "<null>" : safeComponentString(stack.getHoverName()),
                        lines == null ? -1 : lines.size(),
                        isAdvanced,
                        "");
            } catch (Exception ex) {
                Constants.LOG.info("[TooltipDbg] START (stack info failed): {}", ex.toString());
            }
        }

        int i = 0;
        for (ListIterator<Component> it = lines.listIterator(); it.hasNext();) {

            var line = it.next();
            if (dbg)
            {
                Constants.LOG.info("[TooltipDbg] lineIndex={} raw=\"{}\" contentsType={}, siblings={}", i, safeComponentString(line), line == null || line.getContents() == null ? "<null>" : line.getContents().getClass().getSimpleName(), line.getSiblings().size());
                if (line.getSiblings() != null && !line.getSiblings().isEmpty())
                {
                    for (int sidx = 0; sidx < line.getSiblings().size(); sidx++)
                    {
                        var sib = line.getSiblings().get(sidx);
                        Constants.LOG.info("[TooltipDbg]   sibling[{}] type={} str=\"{}\"", sidx, sib == null ? "<null>" : sib.getClass().getSimpleName(), safeComponentString(sib));
                    }
                }
            }

            var content = line.getContents();
            if (content instanceof TranslatableContents ttc)
            {
                if (dbg)
                    Constants.LOG.info("[TooltipDbg]   translatable key={} argsLen={} args={}", ttc.getKey(), ttc.getArgs() == null ? -1 : ttc.getArgs().length, safeArgsSummary(ttc.getArgs()));
                if (ttc.getKey().equals("item.modifiers.mainhand"))
                {
                    atkLineIndex = i+1;
                    if (dbg)
                        Constants.LOG.info("[TooltipDbg]   found mainhand header -> atkLineIndex={}", atkLineIndex);
                }
                // Accessories renders its own attribute block ("accessories.tooltip.attributes.*") for items that
                // are also equippable in an accessory slot. Those lines are worn-equipment lines like the vanilla
                // ones, so they have to flip isArmor too - otherwise they get the held-item treatment (leading
                // space, green) in the middle of an armour tooltip.
                else if ((ttc.getKey().startsWith("item.modifiers.") && !ttc.getKey().contains("mainhand"))
                        || ttc.getKey().startsWith("curios.modifiers")
                        || ttc.getKey().startsWith("accessories.tooltip.attributes"))
                {
                    isArmor = true;
                    if (dbg)
                        Constants.LOG.info("[TooltipDbg]   found non-offhand modifiers header -> isArmor=true (key={})", ttc.getKey());
                }

                if (ttc.getArgs() != null && ttc.getArgs().length == 2 && ttc.getArgs()[1] instanceof MutableComponent mc && mc.getContents() instanceof TranslatableContents tc)
                {
                    if (dbg)
                        Constants.LOG.info("[TooltipDbg]   arg[1] is translatable attribute key={}", tc.getKey());
                    if (tc.getKey().equals("attribute.name.generic.attack_damage"))
                    {
                        atkLineIndex = i;
                        if (dbg)
                            Constants.LOG.info("[TooltipDbg]   matched vanilla attack_damage -> atkLineIndex={}", atkLineIndex);
                    }
                    else if (tc.getKey().equals("attribute.name.generic.armor"))
                    {
                        armorLineIndex = i;
                        isArmor = true;
                        if (dbg)
                            Constants.LOG.info("[TooltipDbg]   matched vanilla armor -> armorLineIndex={} isArmor=true", armorLineIndex);
                    }

                    if (tc.getKey().startsWith("attribute.name.generic"))
                    {
                        if (dbg)
                            Constants.LOG.info("[TooltipDbg]   skipping generic attribute line (keeping vanilla) key={}", tc.getKey());
                        i++;
                        continue;
                    }
                    var splitted = tc.getKey().split("\\.");
                    var attrName = splitted[0];
                    String attrType = null;
                    if (splitted.length > 1)
                        attrType = splitted[1];
                    if (attrName.equals("attribute") && splitted.length > 1)
                    {
                        attrName = splitted[1];
                        if (splitted.length > 2)
                            attrType = splitted[2];
                    }
                    var dc = RPGDamageOverhaulAPI.getDamageClass(attrName);
                    DCAttribute attrToReplace = null;
                    if (dc == null && attrType != null)
                    {
                        var pair = RenameAttributesProperty.getDcForAttribute(ResourceLocation.fromNamespaceAndPath(attrName, attrType));
                        if (pair != null)
                        {
                            dc = pair.getA();
                            attrToReplace = pair.getB();
                        }
                    }
                    if (dbg)
                        Constants.LOG.info("[TooltipDbg]   parsed attrName={} attrType={} damageClass={}", attrName, attrType, dc == null ? "<null>" : dc.name);
                    if (dc != null) {
                        String key = ttc.getKey();
                        // The last segment is the modifier operation ordinal: 0 = ADD_VALUE, 1/2 = the
                        // multiplicative ones. Vanilla already renders 1/2 as a percentage (the lang value
                        // ends in "%%" and the arg is pre-multiplied by 100), so those lines must be left
                        // alone - reformatting them turned FoodAttributes' "+20%" into "+2000%%".
                        boolean additive = key.endsWith(".0");
                        if (additive && !isArmor && (key.startsWith("attribute.modifier.plus") && (attrType == null || !attrType.contains("resistance"))))
                        {
                            key = "attribute.modifier.equals.0";
                            if (dbg)
                                Constants.LOG.info("[TooltipDbg]   normalized plus->equals (non-armor, non-resistance)");
                        }
                        if (key.startsWith("attribute.modifier.minus"))
                            key = key.replace("minus", "take");
                        Object[] args = ttc.getArgs();
                        if (additive && attrType != null && attrType.contains("resistance") && (args[0] instanceof String || args[0] instanceof Component))
                        {
                            String str;
                            if (args[0] instanceof String s)
                                str = s;
                            else
                                str = safeComponentString((Component)args[0]);
                            try {
                                var old = str;
                                args = Arrays.copyOf(args, args.length);
                                args[0] = new DecimalFormat("0.#").format(Double.parseDouble(str) * 100) + "%";
                                if (dbg)
                                    Constants.LOG.info("[TooltipDbg]   resistance % format: {} -> {}", old, args[0]);
                            } catch (Exception ignored) {}
                        }
                        TextColor color = RPGDamageOverhaulAPI.getDamageClassColor(dc, isArmor ? TextColor.fromLegacyFormat(ChatFormatting.BLUE) : TextColor.fromLegacyFormat(ChatFormatting.DARK_GREEN));

                        if (dbg)
                            Constants.LOG.info("[TooltipDbg]   recolouring lineIndex={} in place as damageclass line (key={}, color={})", i, key, color);

                        // Vanilla attribute tooltip args sometimes come pre-styled (blue/red), which overrides
                        // the parent style. Rebuild args as plain text so our damage-class color always wins.
                        // Also, doing the attr name replacement herew
                        Object[] sanitizedArgs = new Object[args.length];
                        for (int ai = 0; ai < args.length; ai++)
                        {
                            Object a = args[ai];
                            if (a instanceof Component comp)
                            {
                                if (comp.equals(mc) && attrToReplace != null)
                                    sanitizedArgs[ai] = Component.literal(Component.translatable(dc.getTranslatableKey(attrToReplace), tc.getArgs()).getString());
                                else
                                    sanitizedArgs[ai] = Component.literal(comp.getString());
                            }
                            else
                                sanitizedArgs[ai] = a;
                        }

                        MutableComponent outLine = (!isArmor ? Component.literal(" ") : Component.literal(""))
                                .append(Component.translatable(key, sanitizedArgs));
                        outLine = outLine.withStyle(Style.EMPTY.withColor(color));
                        // Recoloured where it already sits. Moving these lines to a single anchor used to pull them
                        // out of whichever section they belonged to, which left an empty header behind (Accessories'
                        // "When Equipped:") and stacked lines from different sections next to each other so the same
                        // attribute looked duplicated.
                        it.set(outLine);
                        dcLines.add(outLine);
                    }
                }
            }
            i++;
        }

        if (dcLines.isEmpty())
        {
            if (dbg)
                Constants.LOG.info("[TooltipDbg] END no dcLines produced (no changes)");
            return;
        }
        if (dbg)
        {
            Constants.LOG.info("[TooltipDbg] dcLines={} atkLineIndex={} armorLineIndex={} finalIsArmor={} lines={}", dcLines.size(), atkLineIndex, armorLineIndex, isArmor, lines.size());
            Constants.LOG.info("[TooltipDbg] END linesAfterRecolour={}", lines.size());
            for (int idx = 0; idx < lines.size(); idx++)
                Constants.LOG.info("[TooltipDbg]   OUT[{}]=\"{}\"", idx, safeComponentString(lines.get(idx)));
        }
    }
}
