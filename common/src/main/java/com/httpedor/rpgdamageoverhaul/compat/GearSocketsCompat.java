package com.httpedor.rpgdamageoverhaul.compat;

import java.util.List;
import java.util.Map;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Optional compatibility seam for the GearSockets mod.
 *
 * <p>RPGDO's core never references any GearSockets class. Instead, when GearSockets is present the
 * NeoForge module registers three attachment property types with it and installs a {@link Provider}
 * here that reads those properties off a weapon's filled sockets. The property-reading side lives in
 * the loader module (and touches GearSockets); the arithmetic of how a flat/percentage penetration or
 * conversion combines lives here so it is shared and independent of the mod being installed.
 *
 * <p>Every method is a no-op returning its input when GearSockets is absent, so the hooks in
 * {@link com.httpedor.rpgdamageoverhaul.SharedLogic} and the attack path can call them unconditionally.
 */
public final class GearSocketsCompat {
    private GearSocketsCompat() {}

    /** A single flat-or-percentage effect that targets a damage class, read off one filled socket. */
    public record Entry(DamageClass target, float amount, boolean percentage) {}

    /**
     * Reads the RPGDO attachment properties off a weapon's sockets. Implemented in the loader module
     * against the GearSockets API and installed via {@link #setProvider} only when the mod is loaded.
     * Implementations return {@link List#of()} (no allocation) when a weapon has no relevant sockets.
     */
    public interface Provider {
        List<Entry> armorPenetration(ItemStack weapon);
        List<Entry> resistancePenetration(ItemStack weapon);
        List<Entry> damageConversions(ItemStack weapon);
    }

    private static Provider provider;

    /** Installed once, during mod construction, when GearSockets is present. */
    public static void setProvider(Provider p) {
        provider = p;
    }

    public static boolean isEnabled() {
        return provider != null;
    }

    /** The main-hand item of a damage source's attacker, or {@link ItemStack#EMPTY} when there is none. */
    private static ItemStack attackerWeapon(DamageSource source) {
        if (source != null && source.getEntity() instanceof LivingEntity attacker)
            return attacker.getMainHandItem();
        return ItemStack.EMPTY;
    }

    /** Whether a penetration/conversion aimed at {@code target} applies to incoming {@code dc} damage. */
    private static boolean matches(DamageClass dc, DamageClass target) {
        return dc.name.equals(target.name) || dc.isChildOf(target);
    }

    /**
     * Reduces a defensive value ({@code base}) by every matching penetration entry: percentages combine
     * multiplicatively and flats sum, and the result is clamped to {@code [0, base]}. A non-positive
     * {@code base} (e.g. a negative resistance, meaning vulnerability) is returned untouched so that
     * penetration can never make a target tankier than they already are.
     */
    private static double penetrate(double base, DamageClass dc, List<Entry> entries) {
        if (base <= 0 || entries.isEmpty())
            return base;

        double keepMultiplier = 1.0;
        double flat = 0.0;
        boolean any = false;
        for (Entry e : entries) {
            if (e.target() == null || !matches(dc, e.target()))
                continue;
            any = true;
            if (e.percentage())
                keepMultiplier *= Math.max(0.0, 1.0 - e.amount());
            else
                flat += e.amount();
        }
        if (!any)
            return base;

        return Math.max(0.0, Math.min(base, base * keepMultiplier - flat));
    }

    /** Applies armor penetration from {@code source}'s weapon to {@code armor} for a {@code dc} hit. */
    public static double penetrateArmor(DamageSource source, DamageClass dc, double armor) {
        if (provider == null)
            return armor;
        ItemStack weapon = attackerWeapon(source);
        if (weapon.isEmpty())
            return armor;
        return penetrate(armor, dc, provider.armorPenetration(weapon));
    }

    /** Applies resistance penetration from {@code source}'s weapon to {@code resistance} for a {@code dc} hit. */
    public static double penetrateResistance(DamageSource source, DamageClass dc, double resistance) {
        if (provider == null)
            return resistance;
        ItemStack weapon = attackerWeapon(source);
        if (weapon.isEmpty())
            return resistance;
        return penetrate(resistance, dc, provider.resistancePenetration(weapon));
    }

    /**
     * Splits off the portion of a melee swing that {@code weapon}'s conversion sockets turn into elemental
     * damage. Fills {@code out} with the amount to deal per damage class and returns the total, which the
     * caller subtracts from the physical damage. Percentages are taken of {@code weaponBaseDamage}; the
     * total is scaled down so it can never exceed {@code totalAmount} (the whole swing).
     */
    public static float convertDamage(ItemStack weapon, float weaponBaseDamage, float totalAmount,
                                      Map<DamageClass, Float> out) {
        if (provider == null || weapon == null || weapon.isEmpty() || totalAmount <= 0)
            return 0;

        float converted = 0;
        for (Entry e : provider.damageConversions(weapon)) {
            if (e.target() == null)
                continue;
            float amount = e.percentage() ? e.amount() * weaponBaseDamage : e.amount();
            if (amount <= 0)
                continue;
            converted += amount;
            out.merge(e.target(), amount, Float::sum);
        }

        if (converted > totalAmount && converted > 0) {
            float scale = totalAmount / converted;
            for (Map.Entry<DamageClass, Float> en : out.entrySet())
                en.setValue(en.getValue() * scale);
            converted = totalAmount;
        }
        return converted;
    }
}
