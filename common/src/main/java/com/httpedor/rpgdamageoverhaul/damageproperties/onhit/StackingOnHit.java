package com.httpedor.rpgdamageoverhaul.damageproperties.onhit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.OnHitProperty;
import com.httpedor.rpgdamageoverhaul.ducktypes.DamageClassEntityData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class StackingOnHit extends OnHitProperty
{
    public record StackInstance(long expirationTick, float damage) {}

    private static final String STACKS_TAG = "RPGDOStacks";

    private final int maxStacks;
    private final long stackDuration;
    private final Function<Float, Float> stackDamageFormula;

    public StackingOnHit(int maxStacks, long stackDuration, Function<Float, Float> stackDamageFormula) {
        this.maxStacks = maxStacks;
        this.stackDuration = stackDuration;
        this.stackDamageFormula = stackDamageFormula;
    }

    /**
     * Stacks are timed gameplay state, so they ride along in the entity's NBT and survive a save the way a potion
     * effect does. Expiration ticks are absolute game time, which persists too, so anything that ran out while the
     * entity was unloaded is simply expired on the next hit.
     */
    public static void saveStacks(Map<String, List<StackInstance>> stacks, CompoundTag tag)
    {
        CompoundTag stacksTag = new CompoundTag();
        for (var entry : stacks.entrySet())
        {
            if (entry.getValue().isEmpty())
                continue;
            ListTag list = new ListTag();
            for (var stack : entry.getValue())
            {
                CompoundTag stackTag = new CompoundTag();
                stackTag.putLong("Expires", stack.expirationTick());
                stackTag.putFloat("Damage", stack.damage());
                list.add(stackTag);
            }
            stacksTag.put(entry.getKey(), list);
        }
        if (!stacksTag.isEmpty())
            tag.put(STACKS_TAG, stacksTag);
    }

    public static void loadStacks(Map<String, List<StackInstance>> stacks, CompoundTag tag)
    {
        stacks.clear();
        if (!tag.contains(STACKS_TAG, Tag.TAG_COMPOUND))
            return;

        CompoundTag stacksTag = tag.getCompound(STACKS_TAG);
        for (var dcName : stacksTag.getAllKeys())
        {
            ListTag list = stacksTag.getList(dcName, Tag.TAG_COMPOUND);
            List<StackInstance> loaded = new ArrayList<>();
            for (int i = 0; i < list.size(); i++)
            {
                CompoundTag stackTag = list.getCompound(i);
                loaded.add(new StackInstance(stackTag.getLong("Expires"), stackTag.getFloat("Damage")));
            }
            if (!loaded.isEmpty())
                stacks.put(dcName, loaded);
        }
    }

	@Override
	public void onHit(DamageClass dc, LivingEntity target, DamageSource source, double damage) {

        var targetStacks = DamageClassEntityData.of(target).rpgdo$getDamageClassStacks()
                .computeIfAbsent(dc.name, k -> new ArrayList<>());
        long currentTick = target.level().getGameTime();

        // Drop what has run out, then total up what's left. The stack this hit adds doesn't count towards it.
        targetStacks.removeIf(stack -> stack.expirationTick() <= currentTick);
        float totalDamage = 0;
        for (var stack : targetStacks)
            totalDamage += stack.damage();

        // Deal damage
        var newSource = dc.createDamageSource(source, false);
        target.hurt(newSource, totalDamage);

        // Trigger particle effects
        var particles = dc.getPropertyOfType(ParticlesOnHit.class);
        if (particles != null)
            particles.onHit(dc, target, newSource, totalDamage);

        // Add new stack
        var newInstance = new StackInstance(currentTick + stackDuration, stackDamageFormula.apply((float) damage));
        if (targetStacks.size() < maxStacks)
            targetStacks.add(newInstance);
        else if (!targetStacks.isEmpty())
        {
            // Max stacks reached, so the one closest to running out makes way for the new one.
            int oldestIndex = 0;
            for (int i = 1; i < targetStacks.size(); i++)
            {
                if (targetStacks.get(i).expirationTick() < targetStacks.get(oldestIndex).expirationTick())
                    oldestIndex = i;
            }
            targetStacks.set(oldestIndex, newInstance);
        }
	}

}
