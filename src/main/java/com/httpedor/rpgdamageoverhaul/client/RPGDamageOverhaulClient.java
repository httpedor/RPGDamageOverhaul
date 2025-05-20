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
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;

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
            for (var it = lines.listIterator(); it.hasNext();)
            {
                var txt = it.next();
                var content = txt.getContent();
                if (content instanceof TranslatableTextContent ttc && ttc.getKey().startsWith("attribute.modifier"))
                {
                    var attrName = ((TranslatableTextContent)((MutableText)ttc.getArg(1)).getContent()).getKey();
                    if (attrName.indexOf('.') == -1)
                        return;
                    var dcName = attrName.substring(0, attrName.indexOf('.'));
                    var dc = RPGDamageOverhaulAPI.getDamageClass(dcName);
                    if (dc != null && dc.properties.containsKey("color"))
                    {
                        it.set(((MutableText)txt).formatted(Formatting.byName(dc.properties.get("color").getAsString())));
                    }
                }
            }
        });
    }
}
