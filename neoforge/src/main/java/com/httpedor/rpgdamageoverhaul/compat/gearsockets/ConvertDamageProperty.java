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
 * GearSockets attachment property {@code rpgdamageoverhaul:convert_damage}: converts a flat amount, or a
 * percentage of the weapon's attack damage, into a specific RPGDO damage class when the socketed weapon
 * lands a melee hit. The converted portion is dealt as its own damage source, so the target's per-class
 * armor and resistance apply to it.
 *
 * <pre>{@code
 * { "type": "rpgdamageoverhaul:convert_damage", "damage_class": "fire", "amount": 0.3, "percentage": true }
 * }</pre>
 */
public record ConvertDamageProperty(String damageClass, float amount, boolean percentage)
        implements AttachmentProperty {

    public static final MapCodec<ConvertDamageProperty> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.STRING.fieldOf("damage_class").forGetter(ConvertDamageProperty::damageClass),
            Codec.FLOAT.fieldOf("amount").forGetter(ConvertDamageProperty::amount),
            Codec.BOOL.optionalFieldOf("percentage", false).forGetter(ConvertDamageProperty::percentage)
    ).apply(inst, ConvertDamageProperty::new));

    static AttachmentPropertyType<ConvertDamageProperty> TYPE;

    @Override
    public AttachmentPropertyType<?> type() {
        return TYPE;
    }

    @Override
    public AttachmentProperty amplified(double factor) {
        return new ConvertDamageProperty(damageClass, (float) (amount * factor), percentage);
    }

    @Override
    public void addTooltip(Consumer<Component> lines, @Nullable Player player) {
        Component element = Component.translatable(damageClass + ".damage");
        Component desc = percentage
                ? Component.translatable("rpgdamageoverhaul.gearsockets.convert_damage.percent",
                        EffectText.format(amount * 100.0), element)
                : Component.translatable("rpgdamageoverhaul.gearsockets.convert_damage.flat",
                        EffectText.format(amount), element);
        EffectText.passive(lines, desc, ChatFormatting.RED);
    }
}
