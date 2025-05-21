package com.httpedor.rpgdamageoverhaul.ducktypes;


import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

public interface CopyableDefaultAttrContainer {

    void copyTo(AttributeSupplier.Builder builder);

}
