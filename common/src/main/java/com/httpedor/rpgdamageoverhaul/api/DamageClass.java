package com.httpedor.rpgdamageoverhaul.api;


import com.httpedor.rpgdamageoverhaul.damageproperties.ParentProperty;
import com.httpedor.rpgdamageoverhaul.damageproperties.SimpleProperty;
import com.httpedor.rpgdamageoverhaul.ducktypes.DCDamageSource;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//TODO: Iron's Spells attributes compat
//      Should probably be a DCProperties thing, with a "ironsspellschool" property
public class DamageClass {
    public enum DCAttribute
    {
        DAMAGE,
        ARMOR,
        ABSORPTION,
        /** Amount of {@link #ABSORPTION} restored per second, up to {@link #ABSORPTION_REGEN_MAX}. */
        ABSORPTION_REGEN,
        /** Ceiling that {@link #ABSORPTION_REGEN} regenerates absorption up to; regen never fills past it. */
        ABSORPTION_REGEN_MAX,
        RESISTANCE,
        /** Flat amount of the target's {@link #ARMOR} the attacker ignores when hitting with this class. */
        ARMOR_PENETRATION,
        /** Fraction (0-1) of the target's {@link #ARMOR} the attacker ignores when hitting with this class. */
        ARMOR_PENETRATION_PERCENT,
        /** Fraction (0-1) of a vanilla physical swing the attacker converts into this class' damage. */
        DAMAGE_CONVERSION
    }
    public final String name;
    private final Map<DCAttribute, Holder<Attribute>> attributes;
    public Holder<DamageType> damageType;
    private final List<DamageClassProperty> properties;
    private final Map<Class<? extends DamageClassProperty>, List<DamageClassProperty>> propertiesByClass;
    private final Map<String, DamageClassProperty> propertiesById;

    public DamageClass(String name,
                Map<DCAttribute, Holder<Attribute>> attributes,
                Holder<DamageType> damageType,
                Collection<DamageClassProperty> properties
    )
    {

        this.name = name;
        this.attributes = attributes;
        this.damageType = damageType;
        this.properties = new ArrayList<>(properties);
        this.propertiesByClass = new HashMap<>();
        this.propertiesById = new HashMap<>();
        for (var dcp : properties)
        {
            addProperty(dcp);
        }
    }

    public void addProperty(DamageClassProperty dcp)
    {
        var clazz = dcp.getClass();
        if (!propertiesByClass.containsKey(clazz))
            propertiesByClass.put(clazz, new ArrayList<>());
        propertiesByClass.get(clazz).add(dcp);

        if (dcp instanceof IIdentifiableDamageClassProperty idcp)
        {
            this.propertiesById.put(idcp.getId(), dcp);
        }
    }

    public boolean isChildOf(String parentName)
    {
        DamageClass parent = getParent();
        while (parent != null)
        {
            if (parent.name.equals(parentName))
            {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }
    public boolean isChildOf(DamageClass parent)
    {
        return isChildOf(parent.name);
    }
    public Collection<DamageClassProperty> getProperties()
    {
        return properties;
    }
    @SuppressWarnings("unchecked")
	public <T extends DamageClassProperty> T getPropertyOfType(Class<T> clazz)
    {
        var propsOfClass = propertiesByClass.get(clazz);
        if (propsOfClass == null || propsOfClass.isEmpty())
            return null;

        return (T)propsOfClass.getFirst();
    }
    @SuppressWarnings("unchecked")
	public <T extends DamageClassProperty> Collection<T> getPropertiesOfType(Class<T> clazz)
    {
        List<T> ret = new ArrayList<>();
        for (var prop : properties)
        {
            if (clazz.isAssignableFrom(prop.getClass()))
                ret.add(clazz.cast(prop));
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
	public <T> T getSimplePropertyById(String id)
    {
        SimpleProperty<T> ret = (SimpleProperty<T>)propertiesById.getOrDefault(id, null);
        if (ret == null)
            return null;
        return ret.value();
    }
    public <T> T getSimplePropertyById(String id, T defaultValue)
    {
        T ret = getSimplePropertyById(id);
        if (ret == null)
            return defaultValue;
        return ret;
    }

    public boolean hasPropertyWithId(String id)
    {
        return propertiesById.containsKey(id);
    }
    public boolean hasPropertyOfType(Class<? extends DamageClassProperty> clazz)
    {
        return propertiesByClass.containsKey(clazz);
    }

    public DamageSource createDamageSource(Entity attacker)
    {
        return createDamageSource(attacker, true);
    }
    public DamageSource createDamageSource(Entity attacker, Entity source)
    {
        return createDamageSource(attacker, source, true);
    }
    public DamageSource createDamageSource(Vec3 position)
    {
        return createDamageSource(position, true);
    }
    public DamageSource createDamageSource()
    {
        return createDamageSource(true);
    }

    public DamageSource createDamageSource(Entity attacker, boolean triggerOnHitEffects)
    {
        DamageSource ret = new DamageSource(damageType, attacker);
        ((DCDamageSource)ret).setTriggerOnHitEffects(triggerOnHitEffects);
        return ret;
    }
    public DamageSource createDamageSource(Entity attacker, Entity source, boolean triggerOnHitEffects)
    {
        DamageSource ret = new DamageSource(damageType, attacker, source);
        ((DCDamageSource)ret).setTriggerOnHitEffects(triggerOnHitEffects);
        return ret;
    }
    public DamageSource createDamageSource(Vec3 position, boolean triggerOnHitEffects)
    {
        DamageSource ret = new DamageSource(damageType, position);
        ((DCDamageSource)ret).setTriggerOnHitEffects(triggerOnHitEffects);
        return ret;
    }
    public DamageSource createDamageSource(boolean triggerOnHitEffects)
    {
        DamageSource ret = new DamageSource(damageType);
        ((DCDamageSource)ret).setTriggerOnHitEffects(triggerOnHitEffects);
        return ret;
    }

    public DamageSource createDamageSource(DamageSource original, boolean triggerOnHitEffects)
    {
        DamageSource ret;
        if (original.sourcePositionRaw() != null)
            ret = new DamageSource(damageType, original.sourcePositionRaw());
        else
            ret = new DamageSource(damageType, original.getEntity(), original.getDirectEntity());

        ((DCDamageSource)ret).setTriggerOnHitEffects(triggerOnHitEffects);
        return ret;
    }

    public Holder<Attribute> getAttribute(DCAttribute attr)
    {
        return attributes.get(attr);
    }

    public String getTranslatableKey(DCAttribute attr)
    {
        return name + "." + attr.name().toLowerCase();
    }

    public void applyOnHitEffects(LivingEntity target, DamageSource source, double damage)
    {
        for (var prop : getPropertiesOfType(OnHitProperty.class))
        {
            prop.onHit(this, target, source, damage);
        }
    }

    public DamageClass getParent()
    {
        return RPGDamageOverhaulAPI.getDamageClass(getParentId());
    }
    public String getParentId()
    {
        return getPropertyOfType(ParentProperty.class).parentName;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DamageClass && ((DamageClass) obj).name.equals(name);
    }
}
