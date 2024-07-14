package com.httpedor.rpgdamageoverhaul.ducktypes;

import net.minecraft.entity.attribute.DefaultAttributeContainer;

public interface CopyableDefaultAttrContainer {

    public void copyTo(DefaultAttributeContainer.Builder builder);

}
