package com.httpedor.rpgdamageoverhaul.mixin.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.httpedor.rpgdamageoverhaul.ClientAbsorptionData;
import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Colors the absorption hearts by damage class. The per-class pools are held on the client in
 * {@link ClientAbsorptionData}; vanilla only knows its own (uncolored) absorption. Two hooks work together:
 * <ol>
 * <li>{@code renderHealthLevel} reads {@code getAbsorptionAmount()} once to size the heart rows and to hand the
 *     amount to {@code renderHearts}. Inflating that read by our pooled total makes vanilla lay out and draw the
 *     extra hearts for free.</li>
 * <li>{@code renderHearts} draws each absorbing heart; the hearts past vanilla's own amount are ours, so we tint
 *     each by the class it belongs to. The absorbing hearts are drawn from the top of the bar downwards, so a
 *     simple cursor reproduces each heart's absorption offset without reading vanilla's locals.</li>
 * </ol>
 */
@Mixin(Gui.class)
public abstract class GuiAbsorptionMixin {
    @Unique private int rpgdamageoverhaul$absCursor = Integer.MIN_VALUE;

    // Neutral (white-bodied) heart sprites, so tinting them with setColor yields the class' exact color. Tinting
    // vanilla's gold absorbing sprite can't -- a multiply can never add a channel the sprite lacks (gold has no
    // blue, so a cyan tint came out green).
    @Unique private static final ResourceLocation rpgdamageoverhaul$FULL = ResourceLocation.fromNamespaceAndPath("rpgdamageoverhaul", "hud/heart/rpgdo_absorb_full");
    @Unique private static final ResourceLocation rpgdamageoverhaul$HALF = ResourceLocation.fromNamespaceAndPath("rpgdamageoverhaul", "hud/heart/rpgdo_absorb_half");

    @WrapOperation(method = "renderHealthLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getAbsorptionAmount()F"))
    private float rpgdamageoverhaul$inflateAbsorption(Player player, Operation<Float> original)
    {
        return original.call(player) + rpgdamageoverhaul$ourTotal();
    }

    @Inject(method = "renderHearts", at = @At("HEAD"))
    private void rpgdamageoverhaul$resetCursor(CallbackInfo ci)
    {
        rpgdamageoverhaul$absCursor = Integer.MIN_VALUE;
    }

    @WrapOperation(
            method = "renderHearts",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderHeart(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Gui$HeartType;IIZZZ)V", ordinal = 1))
    private void rpgdamageoverhaul$tintAbsorbingHeart(Gui gui, GuiGraphics guiGraphics, Gui.HeartType heartType, int x, int y, boolean hardcore, boolean halfHeart, boolean blinking, Operation<Void> original)
    {
        Player player = Minecraft.getInstance().player;
        int vanillaUnits = player == null ? 0 : Mth.ceil(player.getAbsorptionAmount());
        float ours = rpgdamageoverhaul$ourTotal();
        int total = (player == null ? 0 : Mth.ceil(player.getAbsorptionAmount() + ours));

        // The absorbing hearts are painted from the highest offset down to 0, two absorption points per heart.
        if (rpgdamageoverhaul$absCursor == Integer.MIN_VALUE)
            rpgdamageoverhaul$absCursor = (Mth.ceil(total / 2.0) - 1) * 2;
        else
            rpgdamageoverhaul$absCursor -= 2;
        int offset = rpgdamageoverhaul$absCursor;

        TextColor color = offset >= vanillaUnits ? rpgdamageoverhaul$colorForUnit(offset - vanillaUnits) : null;
        if (color != null)
        {
            // Ours: draw the neutral heart tinted to the class color instead of the vanilla gold one. Half when
            // this heart holds only the trailing odd absorption point.
            boolean half = offset + 2 > total;
            int rgb = color.getValue();
            RenderSystem.enableBlend();
            guiGraphics.setColor(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f, 1f);
            guiGraphics.blitSprite(half ? rpgdamageoverhaul$HALF : rpgdamageoverhaul$FULL, x, y, 9, 9);
            guiGraphics.setColor(1f, 1f, 1f, 1f);
            RenderSystem.disableBlend();
        }
        else
        {
            // Vanilla's own (real Absorption effect) hearts stay gold.
            original.call(gui, guiGraphics, heartType, x, y, hardcore, halfHeart, blinking);
        }
    }

    @Unique
    private static float rpgdamageoverhaul$ourTotal()
    {
        float total = 0f;
        for (float v : ClientAbsorptionData.get().values())
            total += v;
        return total;
    }

    /**
     * Damage classes that currently have an absorption pool, ordered stably by name, so a given absorption offset
     * maps to a consistent class (and its color) frame to frame.
     */
    @Unique
    private static TextColor rpgdamageoverhaul$colorForUnit(int relativeUnit)
    {
        Map<String, Float> pools = ClientAbsorptionData.get();
        List<String> names = new ArrayList<>(pools.keySet());
        names.sort(null);
        int cumulative = 0;
        for (String name : names)
        {
            cumulative += Math.round(pools.get(name));
            if (relativeUnit < cumulative)
            {
                DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(name);
                return dc == null ? null : RPGDamageOverhaulAPI.getDamageClassColor(dc);
            }
        }
        return null;
    }
}
