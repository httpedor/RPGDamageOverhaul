package com.httpedor.rpgdamageoverhaul;

import java.util.Map;

/**
 * Client-side mirror of the local player's per-damage-class absorption pools, kept up to date by
 * {@link com.httpedor.rpgdamageoverhaul.network.payload.AbsorptionSyncPayload}. The HUD reads it to draw the
 * colored absorption hearts. Holds no client-only types, so it is safe to touch from common code; the reference is
 * swapped wholesale on each update, so readers never see a half-written map.
 */
public final class ClientAbsorptionData {
    private static volatile Map<String, Float> pools = Map.of();

    private ClientAbsorptionData() {}

    public static void set(Map<String, Float> newPools) {
        pools = newPools == null ? Map.of() : newPools;
    }

    public static Map<String, Float> get() {
        return pools;
    }
}
