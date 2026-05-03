package com.httpedor.rpgdamageoverhaul.client;

import com.httpedor.rpgdamageoverhaul.RPGDamageOverhaul;
import com.httpedor.rpgdamageoverhaul.SyncPacket;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.text.DecimalFormat;
import java.util.*;

public class RPGDamageOverhaulClient implements ClientModInitializer {

    private static boolean shouldDebugTooltip(ItemStack stack)
    {
        return false;
        //return stack.getItem() == Items.DIAMOND_CHESTPLATE || stack.getItem() == Items.TRIDENT;
    }

    private static String safeComponentString(Text c)
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
            if (a instanceof Text comp)
                sb.append("(\"").append(safeComponentString(comp)).append("\")");
            else
                sb.append("(").append(String.valueOf(a)).append(")");
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(Identifier.of("rpgdamageoverhaul", "sync"), (client, handler, buf, responseSender) -> {
            SyncPacket packet = SyncPacket.decode(buf);
            packet.handle();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            RPGDamageOverhaulAPI.unloadEverything();
        });

        ItemTooltipCallback.EVENT.register((stack, ctx, lines) -> {
            final boolean dbg = shouldDebugTooltip(stack);
            List<Text> dcLines = new ArrayList<>();
            int atkLineIndex = -1;
            int armorLineIndex = -1;
            boolean isArmor = false;

            if (dbg)
            {
                try {
                    RPGDamageOverhaul.LOGGER.info("[TooltipDbg] START item={} hoverName=\"{}\" lines={} advanced={} filter=\"{}\"", 
                            stack == null ? "<null>" : stack.getTranslationKey(),
                            stack == null ? "<null>" : safeComponentString(stack.toHoverableText()),
                            lines == null ? -1 : lines.size(),
                            ctx.isAdvanced(),
                            "");
                } catch (Exception ex) {
                    RPGDamageOverhaul.LOGGER.info("[TooltipDbg] START (stack info failed): {}", ex.toString());
                }
            }

            int i = 0;
            for (Iterator<Text> it = lines.iterator(); it.hasNext();) {

                var line = it.next();
                if (dbg)
                {
                    RPGDamageOverhaul.LOGGER.info("[TooltipDbg] lineIndex={} raw=\"{}\" contentsType={}, siblings={}", i, safeComponentString(line), line == null || line.getContent() == null ? "<null>" : line.getContent().getClass().getSimpleName(), line.getSiblings().size());
                    if (line.getSiblings() != null && !line.getSiblings().isEmpty())
                    {
                        for (int sidx = 0; sidx < line.getSiblings().size(); sidx++)
                        {
                            var sib = line.getSiblings().get(sidx);
                            RPGDamageOverhaul.LOGGER.info("[TooltipDbg]   sibling[{}] type={} str=\"{}\"", sidx, sib == null ? "<null>" : sib.getClass().getSimpleName(), safeComponentString(sib));
                        }
                    }
                }

                var content = line.getContent();
                if (content instanceof TranslatableTextContent ttc)
                {
                    if (dbg)
                        RPGDamageOverhaul.LOGGER.info("[TooltipDbg]   translatable key={} argsLen={} args={}", ttc.getKey(), ttc.getArgs() == null ? -1 : ttc.getArgs().length, safeArgsSummary(ttc.getArgs()));
                    if (ttc.getKey().equals("item.modifiers.mainhand"))
                    {
                        atkLineIndex = i+1;
                        if (dbg)
                            RPGDamageOverhaul.LOGGER.info("[TooltipDbg]   found mainhand header -> atkLineIndex={}", atkLineIndex);
                    }
                    else if ((ttc.getKey().startsWith("item.modifiers.") && !ttc.getKey().contains("mainhand")) || ttc.getKey().startsWith("curios.modifiers"))
                    {
                        isArmor = true;
                        if (dbg)
                            RPGDamageOverhaul.LOGGER.info("[TooltipDbg]   found non-offhand modifiers header -> isArmor=true (key={})", ttc.getKey());
                    }

                    if (ttc.getArgs() != null && ttc.getArgs().length == 2 && ttc.getArgs()[1] instanceof MutableText mc && mc.getContent() instanceof TranslatableTextContent tc)
                    {
                        if (dbg)
                            RPGDamageOverhaul.LOGGER.info("[TooltipDbg]   arg[1] is translatable attribute key={}", tc.getKey());
                        if (tc.getKey().equals("attribute.name.generic.attack_damage"))
                        {
                            atkLineIndex = i;
                            if (dbg)
                                RPGDamageOverhaul.LOGGER.info("[TooltipDbg]   matched vanilla attack_damage -> atkLineIndex={}", atkLineIndex);
                        }
                        else if (tc.getKey().equals("attribute.name.generic.armor"))
                        {
                            armorLineIndex = i;
                            isArmor = true;
                            if (dbg)
                                RPGDamageOverhaul.LOGGER.info("[TooltipDbg]   matched vanilla armor -> armorLineIndex={} isArmor=true", armorLineIndex);
                        }

                        if (tc.getKey().startsWith("attribute.name.generic"))
                        {
                            if (dbg)
                                RPGDamageOverhaul.LOGGER.info("[TooltipDbg]   skipping generic attribute line (keeping vanilla) key={}", tc.getKey());
                            i++;
                            continue;
                        }
                        var splitted = tc.getKey().split("\\.");
                        var attrName = splitted[0];
                        String attrType = null;
                        if (splitted.length > 1)
                            attrType = splitted[1];
                        var dc = RPGDamageOverhaulAPI.getDamageClass(attrName);
                        if (dbg)
                            RPGDamageOverhaul.LOGGER.info("[TooltipDbg]   parsed attrName={} attrType={} damageClass={}", attrName, attrType, dc == null ? "<null>" : dc.name);
                        if (dc != null) {
                            String key = ttc.getKey();
                            if (!isArmor && (key.startsWith("attribute.modifier.plus") && (attrType == null || !attrType.contains("resistance"))))
                            {
                                key = "attribute.modifier.equals.0";
                                if (dbg)
                                    RPGDamageOverhaul.LOGGER.info("[TooltipDbg]   normalized plus->equals (non-armor, non-resistance)");
                            }
                            if (key.startsWith("attribute.modifier.minus"))
                                key = key.replace("minus", "take");
                            Object[] args = ttc.getArgs();
                            if (attrType != null && attrType.contains("resistance") && (args[0] instanceof String || args[0] instanceof Text))
                            {
                                String str;
                                if (args[0] instanceof String s)
                                    str = s;
                                else
                                    str = safeComponentString((Text)args[0]);
                                try {
                                    var old = str;
                                    args = Arrays.copyOf(args, args.length);
                                    args[0] = new DecimalFormat("0.#").format(Double.parseDouble(str) * 100) + "%";
                                    if (dbg)
                                        RPGDamageOverhaul.LOGGER.info("[TooltipDbg]   resistance % format: {} -> {}", old, args[0]);
                                } catch (Exception ignored) {}
                            }
                            TextColor color = RPGDamageOverhaulAPI.getDamageClassColor(dc, isArmor ? TextColor.fromFormatting(Formatting.BLUE) : TextColor.fromFormatting(Formatting.DARK_GREEN));

                            if (dbg)
                                RPGDamageOverhaul.LOGGER.info("[TooltipDbg]   removing original lineIndex={} and replacing with damageclass line (key={}, color={})", i, key, color);

                            // Vanilla attribute tooltip args sometimes come pre-styled (blue/red), which overrides
                            // the parent style. Rebuild args as plain text so our damage-class color always wins.
                            Object[] sanitizedArgs = new Object[args.length];
                            for (int ai = 0; ai < args.length; ai++)
                            {
                                Object a = args[ai];
                                if (a instanceof Text comp)
                                    sanitizedArgs[ai] = Text.literal(comp.getString());
                                else
                                    sanitizedArgs[ai] = a;
                            }

                            MutableText outLine = (!isArmor ? Text.literal(" ") : Text.literal(""))
                                    .append(Text.translatable(key, sanitizedArgs));
                            outLine = outLine.fillStyle(Style.EMPTY.withColor(color));
                            dcLines.add(outLine);
                            it.remove();
                            i--;
                        }
                    }
                }
                i++;
            }

            if (dcLines.isEmpty())
            {
                if (dbg)
                    RPGDamageOverhaul.LOGGER.info("[TooltipDbg] END no dcLines produced (no changes)");
                return;
            }
            if (dbg)
                RPGDamageOverhaul.LOGGER.info("[TooltipDbg] dcLines={} atkLineIndex={} armorLineIndex={} finalIsArmor={} currentLinesBeforeInsert={}", dcLines.size(), atkLineIndex, armorLineIndex, isArmor, lines.size());
            if (atkLineIndex != -1)
                lines.addAll(Math.min(atkLineIndex + 1, lines.size()-1), dcLines);
            else if (armorLineIndex != -1)
                lines.addAll(Math.min(armorLineIndex + 1, lines.size()-1), dcLines);
            else
                lines.addAll(dcLines);

            if (dbg)
            {
                RPGDamageOverhaul.LOGGER.info("[TooltipDbg] END linesAfterInsert={}", lines.size());
                for (int idx = 0; idx < lines.size(); idx++)
                    RPGDamageOverhaul.LOGGER.info("[TooltipDbg]   OUT[{}]=\"{}\"", idx, safeComponentString(lines.get(idx)));
            }
        });
    }
}
