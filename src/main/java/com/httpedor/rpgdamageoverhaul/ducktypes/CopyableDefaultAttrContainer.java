package com.httpedor.rpgdamageoverhaul.ducktypes;

import net.minecraft.entity.attribute.DefaultAttributeContainer;

public interface CopyableDefaultAttrContainer {

    void copyTo(DefaultAttributeContainer.Builder builder);

}
