package com.httpedor.rpgdamageoverhaul.api;

public abstract class DamageClassProperty
{
    /**
    * Merges this DamageClassProperty with another one of the same type. This is called on the higher or equal priority property.
    * @param other The other DamageClassProperty to merge with. This will always be of the same class as this, and will always have lower or equal priority.
    * @return A new DamageClassProperty that is the result of merging this and other.
    */
    public DamageClassProperty mergeWith(DamageClassProperty other)
    {
        return this;
    }

    /**
    * Called a DamageClass with this property is registered.
    * Example: Used by PotionAttributes property to register attribute modifiers to the potions.
    * @param dc The DamageClass that was registered.
    */
    public void onRegistered(DamageClass dc)
    {

    }
}
