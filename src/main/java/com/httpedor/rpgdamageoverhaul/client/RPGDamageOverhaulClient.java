package com.httpedor.rpgdamageoverhaul.client;

import com.httpedor.rpgdamageoverhaul.RPGDamageOverhaul;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.text.DecimalFormat;
import java.util.*;

@Mod.EventBusSubscriber(modid = RPGDamageOverhaul.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class RPGDamageOverhaulClient {

    @SubscribeEvent
    public void onWorldLeave(ClientPlayerNetworkEvent.LoggingOut e)
    {
        RPGDamageOverhaulAPI.unloadEverything();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tooltipEvent(ItemTooltipEvent e)
    {
        var lines = e.getToolTip();
        List<Component> dcLines = new ArrayList<>();
        int atkLineIndex = -1;
        int armorLineIndex = -1;
        boolean isArmor = false;
        int i = 0;
        for (Iterator<Component> it = lines.iterator(); it.hasNext();) {

            var line = it.next();
            var content = line.getContents();
            if (content instanceof TranslatableContents ttc)
            {
                if (ttc.getKey().equals("item.modifiers.mainhand"))
                    atkLineIndex = i+1;
                else if (ttc.getKey().startsWith("item.modifiers.") && !ttc.getKey().endsWith("offhand"))
                    isArmor = true;

                if (ttc.getArgs() != null && ttc.getArgs().length == 2 && ttc.getArgs()[1] instanceof MutableComponent mc && mc.getContents() instanceof TranslatableContents tc)
                {
                    if (tc.getKey().equals("attribute.name.generic.attack_damage"))
                    {
                        atkLineIndex = i;
                    }
                    else if (tc.getKey().equals("attribute.name.generic.armor"))
                    {
                        armorLineIndex = i;
                        isArmor = true;
                    }

                    if (tc.getKey().startsWith("attribute.name.generic"))
                    {
                        i++;
                        continue;
                    }
                    var splitted = tc.getKey().split("\\.");
                    var attrName = splitted[0];
                    String attrType = null;
                    if (splitted.length > 1)
                        attrType = splitted[1];
                    var dc = RPGDamageOverhaulAPI.getDamageClass(attrName);
                    if (dc != null) {
                        var key = ttc.getKey();
                        if (!isArmor && (key.startsWith("attribute.modifier.plus") && (attrType == null || !attrType.contains("resistance"))))
                            key = "attribute.modifier.equals.0";
                        if (attrType != null && attrType.contains("resistance") && ttc.getArgs()[0] instanceof String str)
                        {
                            try {
                                ttc.getArgs()[0] = new DecimalFormat("0.#").format(Double.parseDouble(str) * 100) + "%";
                            } catch (Exception ignored) {}
                        }
                        TextColor color = RPGDamageOverhaulAPI.getDamageClassColor(dc, isArmor ? TextColor.fromLegacyFormat(ChatFormatting.BLUE) : TextColor.fromLegacyFormat(ChatFormatting.DARK_GREEN));

                        dcLines.add((!isArmor ? Component.literal(" ") : Component.literal("")).append(Component.translatable(key, ttc.getArgs()).withStyle(Style.EMPTY.withColor(color))));
                        it.remove();
                        i--;
                    }
                }
            }
            i++;
        }

        if (dcLines.isEmpty())
            return;
        if (atkLineIndex != -1)
            lines.addAll(Math.min(atkLineIndex + 1, lines.size()-1), dcLines);
        else if (armorLineIndex != -1)
            lines.addAll(Math.min(armorLineIndex + 1, lines.size()-1), dcLines);
        else
            lines.addAll(dcLines);
    }

}
