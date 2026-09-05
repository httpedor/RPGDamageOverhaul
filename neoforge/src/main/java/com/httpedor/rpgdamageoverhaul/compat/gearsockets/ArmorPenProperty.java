package com.httpedor.rpgdamageoverhaul.compat.gearsockets;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.httpedro.gearsockets.attachment.AttachmentProperty;
import com.httpedro.gearsockets.attachment.AttachmentPropertyType;
import com.httpedro.gearsockets.attachment.effect.EffectText;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * GearSockets attachment property {@code rpgdamageoverhaul:armor_pen}: when the socketed weapon deals
 * damage of a given class (or a child of it), it ignores a flat amount or a percentage of the target's
 * armor for that class.
 *
 * <pre>{@code
 * { "type": "rpgdamageoverhaul:armor_pen", "damage_class": "physical", "amount": 0.5, "percentage": true }
 * }</pre>
 */
public record ArmorPenProperty(String damageClass, float amount, boolean percentage)
        implements AttachmentProperty {

    public static final MapCodec<ArmorPenProperty> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.STRING.fieldOf("damage_class").forGetter(ArmorPenProperty::damageClass),
            Codec.FLOAT.fieldOf("amount").forGetter(ArmorPenProperty::amount),
            Codec.BOOL.optionalFieldOf("percentage", false).forGetter(ArmorPenProperty::percentage)
    ).apply(inst, ArmorPenProperty::new));

    static AttachmentPropertyType<ArmorPenProperty> TYPE;

    @Override
    public AttachmentPropertyType<?> type() {
        return TYPE;
    }

    @Override
    public AttachmentProperty amplified(double factor) {
        return new ArmorPenProperty(damageClass, (float) (amount * factor), percentage);
    }

    @Override
    public void addTooltip(Consumer<Component> lines, @Nullable Player player) {
        Component target = Component.translatable(damageClass + ".armor");
        Component desc = percentage
                ? Component.translatable("rpgdamageoverhaul.gearsockets.armor_pen.percent",
                        EffectText.format(amount * 100.0), target)
                : Component.translatable("rpgdamageoverhaul.gearsockets.armor_pen.flat",
                        EffectText.format(amount), target);
        EffectText.passive(lines, desc, ChatFormatting.YELLOW);
    }
}
