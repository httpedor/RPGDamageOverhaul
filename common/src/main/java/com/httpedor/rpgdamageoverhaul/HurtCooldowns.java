package com.httpedor.rpgdamageoverhaul;

import java.util.Iterator;
import java.util.Map;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

/**
 * Per-source invulnerability windows, replacing vanilla's single {@code invulnerableTime} timer.
 * <p>
 * Vanilla gives an entity one i-frame window for <i>all</i> damage, so whatever hits first eats everyone else's
 * damage for the next half second. That is wrong for this mod in two ways: a single swing is split into one
 * {@code hurt} call per damage class, and any two attackers hitting the same target would cancel each other out.
 * <p>
 * Instead every damage source gets its own window, keyed by
 * ({@linkplain DamageSource#typeHolder() damage type}, {@linkplain DamageSource#getEntity() attacker},
 * {@linkplain DamageSource#getDirectEntity() projectile}). So {@code minecraft:lava} and {@code minecraft:cactus}
 * no longer block each other, two players hitting one zombie both land, and three arrows fired by the same player
 * all land because each arrow is a different entity -- while spam-clicking with the same weapon still gets the
 * vanilla half-second cooldown, because the type, the attacker and the direct entity are all unchanged.
 * <p>
 * The numbers stay vanilla's: {@link #remainingTicks} stands in for {@code invulnerableTime} and
 * {@link #lastAmount} for {@code lastHurt}, so {@code LivingEntity#hurt} keeps its own {@code > 10} threshold, its
 * "a bigger hit still deals the difference" rule, and whatever window length NeoForge's damage container asks for.
 */
public final class HurtCooldowns {

    private HurtCooldowns() {}

    /** How often, in ticks, an entity sweeps expired windows out of its map. */
    private static final int PRUNE_INTERVAL = 20;

    /**
     * What counts as "the same source" for i-frame purposes. The damage type is the registry id where there is one
     * (dynamic classes are registered, so there always is) and the message id otherwise; entities are held by id
     * rather than by reference so a window can never keep a dead entity alive.
     */
    public record Key(Object damageType, int causingEntityId, int directEntityId) {}

    /** One open window. Mutable and package-private on purpose -- only the static helpers below touch it. */
    public static final class Entry {
        /** Game time of the hit that opened this window. */
        private long hurtTick;
        /** Ticks the window lasts, i.e. the value vanilla would have put in {@code invulnerableTime}. */
        private int duration;
        /** Biggest amount let through during this window, so a bigger hit still gets the difference. */
        private float lastAmount;
    }

    public static Key keyOf(DamageSource source)
    {
        Object type = source.typeHolder().unwrapKey().<Object>map(ResourceKey::location).orElseGet(source::getMsgId);
        return new Key(type, idOf(source.getEntity()), idOf(source.getDirectEntity()));
    }

    private static int idOf(Entity entity)
    {
        return entity == null ? -1 : entity.getId();
    }

    /** Vanilla's {@code invulnerableTime}, counted down for this source alone. */
    public static int remainingTicks(Map<Key, Entry> cooldowns, DamageSource source, long gameTime)
    {
        Entry entry = cooldowns.get(keyOf(source));
        if (entry == null)
            return 0;
        long left = entry.duration - (gameTime - entry.hurtTick);
        return left <= 0 ? 0 : (int)left;
    }

    /** Vanilla's {@code lastHurt}, for this source alone. */
    public static float lastAmount(Map<Key, Entry> cooldowns, DamageSource source)
    {
        Entry entry = cooldowns.get(keyOf(source));
        return entry == null ? 0f : entry.lastAmount;
    }

    /** Vanilla's {@code this.lastHurt = amount}, for this source alone. */
    public static void recordAmount(Map<Key, Entry> cooldowns, DamageSource source, float amount)
    {
        cooldowns.computeIfAbsent(keyOf(source), key -> new Entry()).lastAmount = amount;
    }

    /** Vanilla's {@code this.invulnerableTime = duration}, for this source alone. */
    public static void openWindow(Map<Key, Entry> cooldowns, DamageSource source, long gameTime, int duration)
    {
        Entry entry = cooldowns.computeIfAbsent(keyOf(source), key -> new Entry());
        entry.hurtTick = gameTime;
        entry.duration = duration;
    }

    /**
     * Drops closed windows. Called from the entity's tick; an entity that is fighting a crowd would otherwise keep
     * an entry per attacker forever.
     */
    public static void prune(Map<Key, Entry> cooldowns, int tickCount, long gameTime)
    {
        if (cooldowns.isEmpty() || tickCount % PRUNE_INTERVAL != 0)
            return;
        Iterator<Entry> it = cooldowns.values().iterator();
        while (it.hasNext())
        {
            Entry entry = it.next();
            if (gameTime - entry.hurtTick >= entry.duration)
                it.remove();
        }
    }
}
