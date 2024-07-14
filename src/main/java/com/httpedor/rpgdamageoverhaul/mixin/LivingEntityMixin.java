package com.httpedor.rpgdamageoverhaul.mixin;

import com.httpedor.rpgdamageoverhaul.DamageClass;
import com.httpedor.rpgdamageoverhaul.DamageHandler;
import com.httpedor.rpgdamageoverhaul.RPGDamageOverhaulAPI;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow protected abstract void applyDamage(DamageSource source, float amount);

    @Shadow public abstract double getAttributeValue(EntityAttribute attribute);

    @Shadow public abstract double getAttributeValue(RegistryEntry<EntityAttribute> attribute);

    @Inject(method = "applyDamage", at = @At("HEAD"), cancellable = true)
    private void damageOverrides(DamageSource source, float amount, CallbackInfo ci)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.getType());
        if (dc != null)
        {
            DamageHandler.executeOnHitEffects(dc, (LivingEntity)((Object)this), source, amount);

            return;
        }

        var newDamages = DamageHandler.applyDamageOverrides((LivingEntity)(Object)this, source, amount);
        if (newDamages != null)
        {
            for (Map.Entry<DamageSource, Double> entry : newDamages.entrySet())
                applyDamage(entry.getKey(), entry.getValue().floatValue());
            ci.cancel();
        }

    }


    @Inject(method = "applyArmorToDamage", at = @At("RETURN"), cancellable = true)
    private void applyDmgTypeArmor(DamageSource source, float amount, CallbackInfoReturnable<Float> cir)
    {
        System.out.println("APPLIED DAMAGE: " + source + " : " + amount + " TO " + this);
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.getType());
        if (dc != null)
        {
            double armor = this.getAttributeValue(dc.armorAttribute);
            DamageClass parent = RPGDamageOverhaulAPI.getDamageClass(dc.parentName);
            while (parent != null)
            {
                armor += this.getAttributeValue(parent.armorAttribute);
                parent = RPGDamageOverhaulAPI.getDamageClass(parent.parentName);
            }
            double armorToughness = this.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS);
            cir.setReturnValue(DamageUtil.getDamageLeft(amount, (float)armor, (float)armorToughness));
        }
    }

    @Inject(method = "modifyAppliedDamage", at = @At("RETURN"), cancellable = true)
    private void applyDmgTypeResistance(DamageSource source, float amount, CallbackInfoReturnable<Float> cir)
    {
        DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.getType());
        if (dc != null)
        {
            double resistance = this.getAttributeValue(dc.resistanceAttribute);
            cir.setReturnValue((float)(amount * (1d - resistance)));
        }
    }
}
