package com.httpedor.rpgdamageoverhaul.damageproperties;

import com.httpedor.rpgdamageoverhaul.api.DamageClassProperty;
import com.httpedor.rpgdamageoverhaul.api.IIdentifiableDamageClassProperty;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class SimpleProperty<T> extends DamageClassProperty implements IIdentifiableDamageClassProperty {
    private final String id;
    private final T val;

    public SimpleProperty(String id, T value)
    {
        this.id = id;
        this.val = value;
    }

	@Override
	public String getId() {
		return id;
	}

	public T value() {
        return val;
    }
}
