package com.httpedor.rpgdamageoverhaul.client;

import com.httpedor.rpgdamageoverhaul.RPGDamageOverhaul;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.event.registry.DynamicRegistryView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.text.DecimalFormat;
import java.util.*;

public class RPGDamageOverhaulClient implements ClientModInitializer {
    private record DTEntry(String identifier, int rawId) {}
    private static final Set<DTEntry> damageTypes = new HashSet<>();
    private static boolean initialized = false;
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(Identifier.of("rpgdamageoverhaul", "damage_type"), (client, handler, buf, responseSender) -> {
            int count = buf.readInt();
            for (int i = 0; i < count; i++)
            {
                String name = buf.readString();
                int rawId = buf.readInt();
                damageTypes.add(new DTEntry(name, rawId));
            }
        });

        ClientTickEvents.START_WORLD_TICK.register((world) -> {
            if (initialized)
                return;
            initialized = true;
            Registry<DamageType> reg = world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE);
            for (DTEntry entry : damageTypes)
            {
                Registry.register(reg, entry.rawId, "rpgdamageoverhaul:" + entry.identifier, new DamageType(entry.identifier, 1));
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            damageTypes.clear();
            initialized = false;
        });

        ItemTooltipCallback.EVENT.register((stack, ctx, lines) -> {
            List<Text> dcLines = new ArrayList<>();
            int atkLineIndex = -1;
            int armorLineIndex = -1;
            boolean isArmor = false;
            int i = 0;
            for (Iterator<Text> it = lines.iterator(); it.hasNext();) {

                var line = it.next();
                var content = line.getContent();
                if (content instanceof TranslatableTextContent ttc)
                {
                    if (ttc.getKey().equals("item.modifiers.mainhand"))
                        atkLineIndex = i+1;
                    else if (ttc.getKey().startsWith("item.modifiers.") && !ttc.getKey().endsWith("offhand"))
                        isArmor = true;

                    if (ttc.getArgs() != null && ttc.getArgs().length == 2 && ttc.getArgs()[1] instanceof MutableText mc && mc.getContent() instanceof TranslatableTextContent tc)
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
                            TextColor color = RPGDamageOverhaulAPI.getDamageClassColor(dc, isArmor ? TextColor.fromFormatting(Formatting.BLUE) : TextColor.fromFormatting(Formatting.DARK_GREEN));

                            dcLines.add((!isArmor ? Text.literal(" ") : Text.literal("")).append(Text.translatable(key, ttc.getArgs()).setStyle(Style.EMPTY.withColor(color))));
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
        });
    }
}
