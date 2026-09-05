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
 * GearSockets attachment property {@code rpgdamageoverhaul:resistance_pen}: when the socketed weapon deals
 * damage of a given class (or a child of it), it ignores a flat amount or a percentage of the target's
 * resistance for that class. Resistance is a fraction in RPGDO, so a flat {@code amount} is subtracted
 * from that fraction and a percentage scales it down.
 *
 * <pre>{@code
 * { "type": "rpgdamageoverhaul:resistance_pen", "damage_class": "fire", "amount": 0.25, "percentage": false }
 * }</pre>
 */
public record ResistancePenProperty(String damageClass, float amount, boolean percentage)
        implements AttachmentProperty {

    public static final MapCodec<ResistancePenProperty> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.STRING.fieldOf("damage_class").forGetter(ResistancePenProperty::damageClass),
            Codec.FLOAT.fieldOf("amount").forGetter(ResistancePenProperty::amount),
            Codec.BOOL.optionalFieldOf("percentage", false).forGetter(ResistancePenProperty::percentage)
    ).apply(inst, ResistancePenProperty::new));

    static AttachmentPropertyType<ResistancePenProperty> TYPE;

    @Override
    public AttachmentPropertyType<?> type() {
        return TYPE;
    }

    @Override
    public AttachmentProperty amplified(double factor) {
        return new ResistancePenProperty(damageClass, (float) (amount * factor), percentage);
    }

    @Override
    public void addTooltip(Consumer<Component> lines, @Nullable Player player) {
        Component target = Component.translatable(damageClass + ".resistance");
        Component desc = percentage
                ? Component.translatable("rpgdamageoverhaul.gearsockets.resistance_pen.percent",
                        EffectText.format(amount * 100.0), target)
                : Component.translatable("rpgdamageoverhaul.gearsockets.resistance_pen.flat",
                        EffectText.format(amount * 100.0), target);
        EffectText.passive(lines, desc, ChatFormatting.YELLOW);
    }
}
