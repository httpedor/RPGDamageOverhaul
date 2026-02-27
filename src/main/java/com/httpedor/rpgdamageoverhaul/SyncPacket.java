package com.httpedor.rpgdamageoverhaul;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI;

import net.minecraft.network.FriendlyByteBuf;

public class SyncPacket {

    private static final Gson GSON = new Gson();

    private final HashMap<String, JsonObject> damageClasses;
    private final JsonObject damageOverrides;
    private final JsonObject entityOverrides;
    private final JsonObject itemOverrides;
    private final JsonObject betterCombatOverrides;

    private SyncPacket(HashMap<String, JsonObject> damageClasses,
                       JsonObject damageOverrides,
                       JsonObject entityOverrides,
                       JsonObject itemOverrides,
                       JsonObject betterCombatOverrides) {
        this.damageClasses = damageClasses;
        this.damageOverrides = damageOverrides;
        this.entityOverrides = entityOverrides;
        this.itemOverrides = itemOverrides;
        this.betterCombatOverrides = betterCombatOverrides;
    }

    public static SyncPacket fromData(DatapackLoader dl) {
        return new SyncPacket(
                dl.dcEntries,
                dl.damageOverrides,
                dl.entityOverrides,
                dl.itemOverrides,
                dl.betterCombatOverrides
        );
    }

    // --- Encoding / Decoding ---

    public void encode(FriendlyByteBuf buf) {
        JsonObject root = new JsonObject();

        JsonObject classesObj = new JsonObject();
        for (var entry : damageClasses.entrySet()) {
            classesObj.add(entry.getKey(), entry.getValue());
        }

        root.add("damageClasses", classesObj);
        root.add("damageOverrides", damageOverrides);
        root.add("entityOverrides", entityOverrides);
        root.add("itemOverrides", itemOverrides);
        root.add("betterCombatOverrides", betterCombatOverrides);

        buf.writeByteArray(compress(GSON.toJson(root)));
    }

    public static SyncPacket decode(FriendlyByteBuf buf) {
        JsonObject root = JsonParser.parseString(decompress(buf.readByteArray()))
                .getAsJsonObject();

        HashMap<String, JsonObject> classes = new HashMap<>();
        JsonObject classesObj = root.getAsJsonObject("damageClasses");
        for (var entry : classesObj.entrySet()) {
            classes.put(entry.getKey(), entry.getValue().getAsJsonObject());
        }

        return new SyncPacket(
                classes,
                root.getAsJsonObject("damageOverrides"),
                root.getAsJsonObject("entityOverrides"),
                root.getAsJsonObject("itemOverrides"),
                root.getAsJsonObject("betterCombatOverrides")
        );
    }

    public void handle() {
        RPGDamageOverhaulAPI.unloadEverything();

        for (var entry : damageClasses.entrySet()) {
            RPGDamageOverhaul.dl.registerDamageClass(entry.getKey(), entry.getValue(), null);
        }

        RPGDamageOverhaul.dl.processDamageOverrides(damageOverrides);
        RPGDamageOverhaul.dl.processEntityOverrides(entityOverrides);
        RPGDamageOverhaul.dl.processBetterCombatOverrides(betterCombatOverrides);
        RPGDamageOverhaul.dl.processItemOverrides(itemOverrides);

        RPGDamageOverhaul.LOGGER.info("Synced from server: {} damage classes, {} damage overrides, {} entity overrides, {} item overrides, {} better combat overrides",
                damageClasses.size(),
                damageOverrides.size(),
                entityOverrides.size(),
                itemOverrides.size(),
                betterCombatOverrides.size());
    }

    private static byte[] compress(String json) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
                gzip.write(json.getBytes(StandardCharsets.UTF_8));
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to compress sync packet", e);
        }
    }

    private static String decompress(byte[] data) {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data))) {
            return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to decompress sync packet", e);
        }
    }
}
