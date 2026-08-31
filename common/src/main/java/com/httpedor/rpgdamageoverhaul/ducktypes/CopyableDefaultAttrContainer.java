package com.httpedor.rpgdamageoverhaul.ducktypes;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

import java.util.Collection;

public interface CopyableDefaultAttrContainer {
    void copyTo(AttributeSupplier.Builder builder);
    Collection<Holder<Attribute>> getAttributes();
}
