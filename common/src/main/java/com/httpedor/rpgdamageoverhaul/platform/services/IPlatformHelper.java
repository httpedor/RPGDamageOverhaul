package com.httpedor.rpgdamageoverhaul.platform.services;

import java.util.Map;

import com.httpedor.rpgdamageoverhaul.api.DamageClass;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

public interface IPlatformHelper {

    /**
     * Gets the name of the current platform
     *
     * @return The name of the current platform.
     */
    String getPlatformName();

    /**
     * Checks if a mod with the given id is loaded.
     *
     * @param modId The mod to check if it is loaded.
     * @return True if the mod is loaded, false otherwise.
     */
    boolean isModLoaded(String modId);

    /**
     * Check if the game is currently in a development environment.
     *
     * @return True if in a development environment, false otherwise.
     */
    boolean isDevelopmentEnvironment();

    void updateEntityAttributes(EntityType<? extends LivingEntity> entityType, AttributeSupplier.Builder builder);
    void fireDamageClassRegisteredEvent(DamageClass dc);

    /** Sends the player their current per-class absorption pools so the client HUD can draw the colored hearts. */
    void syncAbsorptionPools(ServerPlayer player, Map<String, Float> pools);

    /**
     * Gets the name of the environment type as a string.
     *
     * @return The name of the environment type.
     */
    default String getEnvironmentName() {

        return isDevelopmentEnvironment() ? "development" : "production";
    }
}