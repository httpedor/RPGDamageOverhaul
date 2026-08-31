package com.httpedor.rpgdamageoverhaul.damageproperties.onhit;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.httpedor.rpgdamageoverhaul.Constants;
import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.OnHitProperty;
import com.httpedor.rpgdamageoverhaul.ducktypes.DamageClassEntityData;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class AttributeModifierOnHit extends OnHitProperty {
    public enum ReplaceType {
        ALWAYS,
        LOWER,
        HIGHER
    }
    public record AttributeModifierData(Holder<Attribute> attribute, ReplaceType replaceType, Function<Float, Float> amountFunction, Function<Float, Float> durationFunction, AttributeModifier.Operation operation) {}
    /** One timed modifier this mod put on an entity, tracked so it can be taken back off when it runs out. */
    public record ActiveModifier(Holder<Attribute> attribute, long expirationTick) {}

    private List<AttributeModifierData> modifiers;

    public AttributeModifierOnHit(List<AttributeModifierData> modifiers)
    {
        this.modifiers = modifiers;
    }

    public static void pruneExpired(LivingEntity entity)
    {
        var modifiers = DamageClassEntityData.of(entity).rpgdo$getActiveAttributeModifiers();
        if (modifiers.isEmpty())
            return;
        long currentTime = entity.level().getGameTime();
        for (var entry : new ArrayList<>(modifiers.entrySet()))
        {
            var active = entry.getValue();
            var modId = entry.getKey();
            if (currentTime >= active.expirationTick())
            {
                var attrInstance = entity.getAttribute(active.attribute());
                if (attrInstance != null)
                {
                    Float healthRatio = null;
                    // Compare the attribute, not the instance: an AttributeInstance is never == a Holder, so this
                    // check used to be dead and max health snapped the entity's current health around on expiry.
                    if (active.attribute().equals(Attributes.MAX_HEALTH))
                        healthRatio = entity.getHealth() / entity.getMaxHealth();
                    attrInstance.removeModifier(modId);
                    if (healthRatio != null)
                        entity.setHealth(entity.getMaxHealth() * healthRatio);
                }
                modifiers.remove(modId);
            }
        }
    }

    @Override
    public void onHit(DamageClass dc, LivingEntity target, DamageSource source, double damage) {
        int i = 0;
        for (AttributeModifierData data : modifiers)
        {
            ResourceLocation attrId = data.attribute().unwrapKey().orElse(ResourceKey.create(Registries.ATTRIBUTE, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "unknown_attribute"))).location();
            var attr = target.getAttribute(data.attribute());
            if (attr == null)
                continue;
            var modifier = new AttributeModifier(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, dc.name + "/" + attrId.getPath() + "/" + i), data.amountFunction().apply((float) damage), data.operation());
            var existingModifier = attr.getModifier(modifier.id());
            long duration = (long) (data.durationFunction().apply((float) damage) * 20);
            long expirationTick = target.level().getGameTime() + duration;
            if (existingModifier != null)
            {
                boolean shouldReplace = false;
                switch (data.replaceType())
                {
                    case ALWAYS -> shouldReplace = true;
                    case LOWER -> shouldReplace = modifier.amount() < existingModifier.amount();
                    case HIGHER -> shouldReplace = modifier.amount() > existingModifier.amount();
                }
                if (shouldReplace)
                {
                    attr.removeModifier(existingModifier);
                    attr.addTransientModifier(modifier);
                    DamageClassEntityData.of(target).rpgdo$getActiveAttributeModifiers()
                            .put(modifier.id(), new ActiveModifier(data.attribute(), expirationTick));
                }
            }
            else
            {
                attr.addTransientModifier(modifier);
                DamageClassEntityData.of(target).rpgdo$getActiveAttributeModifiers()
                        .put(modifier.id(), new ActiveModifier(data.attribute(), expirationTick));
            }
            i++;
        }
    }
}
