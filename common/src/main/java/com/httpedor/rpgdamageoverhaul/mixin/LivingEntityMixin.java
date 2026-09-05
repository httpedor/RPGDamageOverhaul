package com.httpedor.rpgdamageoverhaul.mixin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.httpedor.rpgdamageoverhaul.HurtCooldowns;
import com.httpedor.rpgdamageoverhaul.SharedLogic;
import com.httpedor.rpgdamageoverhaul.platform.Services;
import com.httpedor.rpgdamageoverhaul.ducktypes.CopyableDefaultAttrContainer;
import net.minecraft.server.level.ServerPlayer;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;
import com.httpedor.rpgdamageoverhaul.damageproperties.onhit.AttributeModifierOnHit;
import com.httpedor.rpgdamageoverhaul.damageproperties.onhit.HealingModifierOnHit;
import com.httpedor.rpgdamageoverhaul.damageproperties.onhit.ModifyDamageOnHit;
import com.httpedor.rpgdamageoverhaul.damageproperties.onhit.StackingOnHit;
import com.httpedor.rpgdamageoverhaul.ducktypes.DamageClassEntityData;
import com.httpedor.rpgdamageoverhaul.ducktypes.DCDamageSource;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.level.Level;

@Mixin(value = LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements DamageClassEntityData {

    private final Map<HurtCooldowns.Key, HurtCooldowns.Entry> rpgdamageoverhaul$hurtCooldowns = new HashMap<>();
    private final Map<String, List<StackingOnHit.StackInstance>> rpgdamageoverhaul$stacks = new HashMap<>();

    private final Map<ResourceLocation, AttributeModifierOnHit.ActiveModifier> rpgdamageoverhaul$activeModifiers = new HashMap<>();

    @Override
    public Map<HurtCooldowns.Key, HurtCooldowns.Entry> rpgdo$getHurtCooldowns() {
        return rpgdamageoverhaul$hurtCooldowns;
    }

    @Override
    public Map<String, List<StackingOnHit.StackInstance>> rpgdo$getDamageClassStacks() {
        return rpgdamageoverhaul$stacks;
    }

    @Override
    public Map<ResourceLocation, AttributeModifierOnHit.ActiveModifier> rpgdo$getActiveAttributeModifiers() {
        return rpgdamageoverhaul$activeModifiers;
    }

    private final Map<HealingModifierOnHit, HealingModifierOnHit.ActiveHealModifier> rpgdamageoverhaul$healModifiers = new HashMap<>();
    private final Map<ModifyDamageOnHit, ModifyDamageOnHit.DamageModificationInstance> rpgdamageoverhaul$damageModifications = new HashMap<>();
    private long rpgdamageoverhaul$wetUntil = 0;
    private final Map<String, Float> rpgdamageoverhaul$absorptionPools = new HashMap<>();
    private final Map<String, Float> rpgdamageoverhaul$lastAbsorptionGranted = new HashMap<>();
    /** Last pools pushed to the client, rounded to half-hearts, so regen doesn't spam a packet every tick. Players only. */
    private Map<String, Integer> rpgdamageoverhaul$lastSyncedAbsorption = null;

    @Override
    public Map<String, Float> rpgdo$getAbsorptionPools() {
        return rpgdamageoverhaul$absorptionPools;
    }

    @Override
    public Map<String, Float> rpgdo$getLastAbsorptionGranted() {
        return rpgdamageoverhaul$lastAbsorptionGranted;
    }

    @Override
    public Map<HealingModifierOnHit, HealingModifierOnHit.ActiveHealModifier> rpgdo$getActiveHealModifiers() {
        return rpgdamageoverhaul$healModifiers;
    }

    @Override
    public Map<ModifyDamageOnHit, ModifyDamageOnHit.DamageModificationInstance> rpgdo$getDamageModifications() {
        return rpgdamageoverhaul$damageModifications;
    }

    @Override
    public long rpgdo$getWetUntil() {
        return rpgdamageoverhaul$wetUntil;
    }

    @Override
    public void rpgdo$setWetUntil(long gameTime) {
        rpgdamageoverhaul$wetUntil = gameTime;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void rpgdamageoverhaul$saveStacks(CompoundTag tag, CallbackInfo ci) {
        StackingOnHit.saveStacks(rpgdamageoverhaul$stacks, tag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void rpgdamageoverhaul$loadStacks(CompoundTag tag, CallbackInfo ci) {
        StackingOnHit.loadStacks(rpgdamageoverhaul$stacks, tag);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void rpgdamageoverhaul$saveAbsorption(CompoundTag tag, CallbackInfo ci) {
        if (rpgdamageoverhaul$absorptionPools.isEmpty() && rpgdamageoverhaul$lastAbsorptionGranted.isEmpty())
            return;
        CompoundTag pools = new CompoundTag();
        for (var e : rpgdamageoverhaul$absorptionPools.entrySet())
            pools.putFloat(e.getKey(), e.getValue());
        CompoundTag granted = new CompoundTag();
        for (var e : rpgdamageoverhaul$lastAbsorptionGranted.entrySet())
            granted.putFloat(e.getKey(), e.getValue());
        CompoundTag root = new CompoundTag();
        root.put("pools", pools);
        root.put("lastGranted", granted);
        tag.put("rpgdo_absorption", root);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void rpgdamageoverhaul$loadAbsorption(CompoundTag tag, CallbackInfo ci) {
        rpgdamageoverhaul$absorptionPools.clear();
        rpgdamageoverhaul$lastAbsorptionGranted.clear();
        if (!tag.contains("rpgdo_absorption"))
            return;
        CompoundTag root = tag.getCompound("rpgdo_absorption");
        CompoundTag pools = root.getCompound("pools");
        for (var key : pools.getAllKeys())
            rpgdamageoverhaul$absorptionPools.put(key, pools.getFloat(key));
        CompoundTag granted = root.getCompound("lastGranted");
        for (var key : granted.getAllKeys())
            rpgdamageoverhaul$lastAbsorptionGranted.put(key, granted.getFloat(key));
    }

    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Shadow protected abstract void actuallyHurt(DamageSource source, float amount);

    @Shadow public abstract double getAttributeValue(Holder<Attribute> attribute);
    @Shadow public abstract AttributeInstance getAttribute(Holder<Attribute> attribute);

    @Shadow public abstract float getHealth();


    @Shadow public abstract float getMaxHealth();

    @Shadow public abstract void setHealth(float p_21154_);

    @Shadow
    public abstract AttributeMap getAttributes();

    // Vanilla keeps one i-frame timer (invulnerableTime) and one damage high-water mark (lastHurt) for the whole
    // entity, so the first hit of a half second swallows every other hit -- which breaks this mod outright, since a
    // single swing arrives as one hurt() call per damage class, and breaks two players hitting the same mob.
    // The four hooks below leave hurt()'s logic alone and just point those two fields at a per-source window
    // instead (see HurtCooldowns), so the vanilla "> 10 ticks" threshold and the "a bigger hit still deals the
    // difference" rule keep working, per damage type, per attacker, per projectile.
    // The real invulnerableTime field is still written, because the client reads it for the hurt flash and the
    // health-bar blink; it just no longer gates anything.

    @ModifyExpressionValue(method = "hurt", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;invulnerableTime:I", opcode = Opcodes.GETFIELD))
    private int rpgdamageoverhaul$iframesPerSource(int original, @Local(argsOnly = true) DamageSource source)
    {
        return HurtCooldowns.remainingTicks(rpgdamageoverhaul$hurtCooldowns, source, level().getGameTime());
    }

    @ModifyExpressionValue(method = "hurt", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;lastHurt:F", opcode = Opcodes.GETFIELD))
    private float rpgdamageoverhaul$lastHurtPerSource(float original, @Local(argsOnly = true) DamageSource source)
    {
        return HurtCooldowns.lastAmount(rpgdamageoverhaul$hurtCooldowns, source);
    }

    @WrapOperation(method = "hurt", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;lastHurt:F", opcode = Opcodes.PUTFIELD))
    private void rpgdamageoverhaul$recordLastHurt(LivingEntity self, float amount, Operation<Void> original, @Local(argsOnly = true) DamageSource source)
    {
        original.call(self, amount);
        HurtCooldowns.recordAmount(rpgdamageoverhaul$hurtCooldowns, source, amount);
    }

    @WrapOperation(method = "hurt", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;invulnerableTime:I", opcode = Opcodes.PUTFIELD))
    private void rpgdamageoverhaul$openIFrameWindow(LivingEntity self, int duration, Operation<Void> original, @Local(argsOnly = true) DamageSource source)
    {
        original.call(self, duration);
        HurtCooldowns.openWindow(rpgdamageoverhaul$hurtCooldowns, source, level().getGameTime(), duration);
    }

    @WrapOperation(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z"))
    private boolean blockFireResistance(LivingEntity instance, Holder<net.minecraft.world.effect.MobEffect> effect, Operation<Boolean> original)
    {
        if (effect == MobEffects.FIRE_RESISTANCE)
            return false;
        return original.call(instance, effect);
    }

    @ModifyVariable(method = "actuallyHurt", at = @At(value = "HEAD"), argsOnly = true)
    private float modifyDamage(float amount, @Local(argsOnly = true) DamageSource source)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.type());
        if (dc != null)
            return SharedLogic.modifyDamagePreArmor(amount, (LivingEntity)(Object)this, dc);
        return amount;
    }

    @Inject(method = "actuallyHurt", at = @At("HEAD"), cancellable = true)
    private void damageOverrides(DamageSource source, float amount, CallbackInfo ci)
    {
        if (RPGDamageOverhaulAPI.isRPGDamageType(source.typeHolder()))
            return;

        // Entity overrides
        // Only for projectiles, as melee attacks will already deal RPGDO damage through the doHurtTarget injection at MobEntityMixin
        Map<DamageClass, Double> newDcs = null;
        if (source.getDirectEntity() != null)
        {
            newDcs = RPGDamageOverhaulAPI.getEntityOverrides(source.getDirectEntity());
            if (newDcs != null && !newDcs.isEmpty())
            {
                for (var entry : newDcs.entrySet())
                {
                    var dc = entry.getKey();
                    float newDamage = (float) (amount * entry.getValue());
                    /*if (source.getEntity() != null && source.getEntity() instanceof LivingEntity le)
                    {
                        // Not using SharedLogic.getAttributeValue here because then spell power would be applied twice, once in the damage calculation of the spell itself, and once here. This way, the spell power is only applied once.
                        var attrInstance = le.getAttribute(dc.getAttribute(DCAttribute.DAMAGE));
                        if (attrInstance != null)
                            newDamage = AttributeUtils.applyAttributeModifiers(newDamage, attrInstance.getModifiers());
                    }*/
                    actuallyHurt(dc.createDamageSource(source.getDirectEntity(), source.getEntity()), newDamage);
                }
                ci.cancel();
                return;
            }
        }

        // DamageType overrides
        if (newDcs == null || newDcs.isEmpty())
        {
            var newDamages = RPGDamageOverhaulAPI.applyDamageOverrides(source, amount);
            if (newDamages != null && !newDamages.isEmpty())
            {
                for (Map.Entry<DamageSource, Double> entry : newDamages.entrySet())
                {
                    actuallyHurt(entry.getKey(), entry.getValue().floatValue());
                }
                ci.cancel();
            }
        }
    }

    @Inject(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"))
    private void onHitEffects(DamageSource source, float amount, CallbackInfo ci)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.type());
        if (dc != null)
        {
            if (((DCDamageSource)source).shouldTriggerOnHitEffects())
                dc.applyOnHitEffects((LivingEntity)((Object)this), source, amount);
        }
    }

    @WrapOperation(method = "getDamageAfterArmorAbsorb", at = @At(value="INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean noDefaultArmor(DamageSource instance, TagKey<DamageType> tag, Operation<Boolean> original)
    {
        if (RPGDamageOverhaulAPI.isRPGDamageType(instance.typeHolder()))
            return true;
        return original.call(instance, tag);
    }

    @WrapOperation(method = "getDamageAfterMagicAbsorb", at = @At(value="INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z", ordinal = 1))
    private boolean noResistancePot(DamageSource instance, TagKey<DamageType> tag, Operation<Boolean> original)
    {
        var dc = RPGDamageOverhaulAPI.getDamageClass(instance.type());
        if (dc != null && !dc.isChildOf("physical"))
            return true;
        return original.call(instance, tag);
    }

    @Inject(method = "getDamageAfterArmorAbsorb", at = @At("HEAD"), cancellable = true)
    private void applyDmgTypeArmor(DamageSource source, float amount, CallbackInfoReturnable<Float> cir)
    {
        cir.setReturnValue(SharedLogic.getDamagePostArmor(amount, (LivingEntity)(Object)this, source));
    }

    @Inject(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"))
    private void logDamage(DamageSource source, float amount, CallbackInfo ci)
    {
        //System.out.println("APPLIED DAMAGE: (" + source.type().msgId() + ", " + (source.getDirectEntity() != null ? ForgeRegistries.ENTITY_TYPES.getKey(source.getDirectEntity().getType()) : "NULL") + ", " + (source.getEntity() != null ? ForgeRegistries.ENTITY_TYPES.getKey(source.getEntity().getType()) : "NULL") + ") : " + amount + " TO " + this.getType());
    }

    @ModifyVariable(method = "setHealth", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float applyHealBlock(float health)
    {
        if (health > getHealth())
            health = getHealth() + HealingModifierOnHit.modifyHealing((LivingEntity)(Object)this, health - getHealth());
        return health;
    }

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void removeModifiers(CallbackInfo ci)
    {
        if (level().isClientSide)
            return;

        AttributeModifierOnHit.pruneExpired((LivingEntity)(Object)this);
        HurtCooldowns.prune(rpgdamageoverhaul$hurtCooldowns, tickCount, level().getGameTime());

        LivingEntity self = (LivingEntity)(Object)this;
        SharedLogic.tickAbsorption(self);
        if (self instanceof ServerPlayer sp)
        {
            // Only resync when a pool crosses a half-heart boundary, so passive regen doesn't send a packet a tick.
            Map<String, Integer> rounded = new HashMap<>();
            for (var e : rpgdamageoverhaul$absorptionPools.entrySet())
                rounded.put(e.getKey(), Math.round(e.getValue()));
            if (!rounded.equals(rpgdamageoverhaul$lastSyncedAbsorption))
            {
                Services.PLATFORM.syncAbsorptionPools(sp, rpgdamageoverhaul$absorptionPools);
                rpgdamageoverhaul$lastSyncedAbsorption = rounded;
            }
        }
    }
}
