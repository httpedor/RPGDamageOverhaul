package com.httpedor.rpgdamageoverhaul.compat.gearsockets;

import java.util.ArrayList;
import java.util.List;

import com.httpedor.rpgdamageoverhaul.Constants;
import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.httpedor.rpgdamageoverhaul.compat.GearSocketsCompat;
import com.httpedro.gearsockets.attachment.AttachmentPropertyTypes;
import com.httpedro.gearsockets.attachment.SocketProperties;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Wires RPGDO into GearSockets. Loaded and called from the mod constructor <em>only</em> when GearSockets
 * is present, so no GearSockets class is ever resolved otherwise. Registering the property types here (at
 * construction) is safe: attachment JSON is parsed no earlier than datapack load, well after all mods have
 * been constructed.
 */
public final class GearSocketsCompatInit {
    private GearSocketsCompatInit() {}

    public static void init() {
        ConvertDamageProperty.TYPE = AttachmentPropertyTypes.register(
                id("convert_damage"), ConvertDamageProperty.CODEC);
        ArmorPenProperty.TYPE = AttachmentPropertyTypes.register(
                id("armor_pen"), ArmorPenProperty.CODEC);
        ResistancePenProperty.TYPE = AttachmentPropertyTypes.register(
                id("resistance_pen"), ResistancePenProperty.CODEC);

        GearSocketsCompat.setProvider(new Provider());
        Constants.LOG.info("GearSockets compatibility enabled.");
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
    }

    /** Reads RPGDO attachment properties off a weapon's filled sockets and resolves their damage classes. */
    private static final class Provider implements GearSocketsCompat.Provider {
        @Override
        public List<GearSocketsCompat.Entry> armorPenetration(ItemStack weapon) {
            List<ArmorPenProperty> props = SocketProperties.of(weapon, ArmorPenProperty.class);
            if (props.isEmpty())
                return List.of();
            List<GearSocketsCompat.Entry> out = new ArrayList<>(props.size());
            for (ArmorPenProperty p : props)
                addEntry(out, p.damageClass(), p.amount(), p.percentage());
            return out;
        }

        @Override
        public List<GearSocketsCompat.Entry> resistancePenetration(ItemStack weapon) {
            List<ResistancePenProperty> props = SocketProperties.of(weapon, ResistancePenProperty.class);
            if (props.isEmpty())
                return List.of();
            List<GearSocketsCompat.Entry> out = new ArrayList<>(props.size());
            for (ResistancePenProperty p : props)
                addEntry(out, p.damageClass(), p.amount(), p.percentage());
            return out;
        }

        @Override
        public List<GearSocketsCompat.Entry> damageConversions(ItemStack weapon) {
            List<ConvertDamageProperty> props = SocketProperties.of(weapon, ConvertDamageProperty.class);
            if (props.isEmpty())
                return List.of();
            List<GearSocketsCompat.Entry> out = new ArrayList<>(props.size());
            for (ConvertDamageProperty p : props)
                addEntry(out, p.damageClass(), p.amount(), p.percentage());
            return out;
        }

        private static void addEntry(List<GearSocketsCompat.Entry> out, String className,
                                     float amount, boolean percentage) {
            DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(className);
            if (dc == null) {
                Constants.LOG.warn("GearSockets attachment references unknown RPGDO damage class '{}', ignoring.", className);
                return;
            }
            out.add(new GearSocketsCompat.Entry(dc, amount, percentage));
        }
    }
}
