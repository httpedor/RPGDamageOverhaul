package com.httpedor.rpgdamageoverhaul.compat.jade;

import com.httpedor.rpgdamageoverhaul.RPGDamageOverhaul;
import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

import java.text.DecimalFormat;

public enum ResistanceComponentProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip iTooltip, EntityAccessor entityAccessor, IPluginConfig iPluginConfig) {
        ITooltip box = IElementHelper.get().tooltip();
        var resistancesNbt = entityAccessor.getServerData().getCompound("Resistances");
        for (var dcName : resistancesNbt.getAllKeys())
        {
            DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(dcName);
            double val = resistancesNbt.getDouble(dcName);
            String lang = val > 0 ? "attribute.modifier.plus.1" : "attribute.modifier.take.1";
            box.add(Component.translatable(lang, new DecimalFormat("0.#").format(Math.abs(val * 100)), Component.translatable(dc.resistanceAttribute.getDescriptionId())).withStyle(Style.EMPTY.withColor(RPGDamageOverhaulAPI.getDamageClassColor(dc))));
        }
        iTooltip.add(IElementHelper.get().box(box, BoxStyle.DEFAULT));
    }

    @Override
    public ResourceLocation getUid() {
        return new ResourceLocation(RPGDamageOverhaul.MODID, "resistances");
    }

    @Override
    public void appendServerData(CompoundTag nbt, EntityAccessor entityAccessor) {
        LivingEntity le = (LivingEntity) entityAccessor.getEntity();
        CompoundTag map = new CompoundTag();
        for (var dc : RPGDamageOverhaulAPI.getAllDamageClasses())
        {
            if (le.getAttribute(dc.resistanceAttribute) == null)
                continue;
            var val = le.getAttributeValue(dc.resistanceAttribute);
            if (val == 0)
                continue;
            map.putDouble(dc.name, val);
        }
        nbt.put("Resistances", map);
    }
}
