package com.httpedor.rpgdamageoverhaul;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class RPGDamageOverhaul implements ModInitializer {

    private void spawnHitParticles(ServerWorld world, ParticleEffect parameters, double x, double y, double z, int amount)
    {
        double maxSpeed  = 0.08;
        for (int i = 0; i < amount; i++)
        {
            world.spawnParticles(parameters, x, y, z, amount, 0d, 0d, 0d, maxSpeed);
        }
    }

    @Override
    public void onInitialize() {

        RPGDamageOverhaulAPI.registerOnHitEffect(new Identifier("rpgdamageoverhaul", "particles"), (target, source, dmg) -> {
            DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(source.getType());
            var particleEl = dc.properties.getOrDefault("particle", null);
            if (particleEl == null)
                return;

            String particleId = particleEl.getAsString();
            if (!target.getWorld().isClient)
            {
                DefaultParticleType pt = (DefaultParticleType) Registries.PARTICLE_TYPE.get(new Identifier(particleId));
                if (pt == null)
                    System.out.println("Particle not found: " + particleId);

                try {
                    spawnHitParticles((ServerWorld) target.getWorld(), pt.getParametersFactory().read(pt, new StringReader("")), target.getX(), target.getEyeY(), target.getZ(), (int) (dmg/2)+1);
                } catch (CommandSyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        RPGDamageOverhaulAPI.registerOnHitEffect(new Identifier("rpgdamageoverhaul", "set_fire"), (target, source, dmg) -> {
            target.setFireTicks(target.getFireTicks() + (int) Math.round(dmg/3 * 20));
            target.setFrozenTicks(0);
        });
        RPGDamageOverhaulAPI.registerOnHitEffect(new Identifier("rpgdamageoverhaul", "set_frozen"), (target, source, dmg) -> {
            target.setFrozenTicks((int) Math.round(dmg/1.5 * 20));
            target.setFireTicks(0);
        });

        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return new Identifier("rpgdamageoverhaul", "read_damage_types");
            }

            private void registerDamageClass(String name, JsonObject obj, DamageClass parent)
            {
                List<Identifier> onHitEffects = new ArrayList<>();
                if (obj.has("onHit"))
                    onHitEffects = obj.getAsJsonArray("onHit").asList().stream().map(JsonElement::getAsString).map(Identifier::new).toList();

                DamageClass dc = RPGDamageOverhaulAPI.registerDamage(name, parent == null ? null : parent.name);
                dc.properties = obj.asMap();

                for (Identifier effect : onHitEffects)
                {
                    dc.addOnHitEffect(effect);
                }

                if (obj.has("subClasses")) {
                    for (Map.Entry<String, JsonElement> subClass : obj.getAsJsonObject("subClasses").entrySet()) {
                        registerDamageClass(subClass.getKey(), subClass.getValue().getAsJsonObject(), RPGDamageOverhaulAPI.getDamageClass(name));
                    }
                }

            }

            @Override
            public void reload(ResourceManager manager) {
                
                //Read damage classes
                for (Map.Entry<Identifier, Resource> entry : manager.findResources("rpgdamageoverhaul", path -> path.getPath().equals("rpgdamageoverhaul/damage_classes.json")).entrySet())
                {
                    try (InputStream stream = manager.getResource(entry.getKey()).get().getInputStream()) {
                        InputStreamReader reader = new InputStreamReader(stream);
                        JsonObject obj = (JsonObject) JsonParser.parseReader(reader);
                        for (Map.Entry<String, JsonElement> dcJson : obj.entrySet())
                        {
                            registerDamageClass(dcJson.getKey(), dcJson.getValue().getAsJsonObject(), null);
                        }
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }

                //Read damage overrides
                for (Map.Entry<Identifier, Resource> entry : manager.findResources("rpgdamageoverhaul", path -> path.equals("rpgdamageoverhaul/damage_overrides.json")).entrySet())
                {
                    try (InputStream stream = manager.getResource(entry.getKey()).get().getInputStream()) {
                        InputStreamReader reader = new InputStreamReader(stream);
                        JsonObject obj = (JsonObject) JsonParser.parseReader(reader);
                        for (Map.Entry<String, JsonElement> override : obj.entrySet())
                        {
                            Identifier mcDamageType = new Identifier(override.getKey());
                            Map<DamageClass, Double> overrides = new HashMap<>();
                            for (Map.Entry<String, JsonElement> overrideEntry : override.getValue().getAsJsonObject().entrySet())
                            {
                                DamageClass dc = RPGDamageOverhaulAPI.getDamageClass(overrideEntry.getKey());
                                if (dc != null)
                                    overrides.put(dc, overrideEntry.getValue().getAsDouble());
                            }
                            RPGDamageOverhaulAPI.registerOverride(mcDamageType, overrides);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

    }
}
