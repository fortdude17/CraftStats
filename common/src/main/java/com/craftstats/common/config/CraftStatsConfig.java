package com.craftstats.common.config;

import com.craftstats.common.CraftStats;
import com.craftstats.common.stats.TargetType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.architectury.platform.Platform;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code config/craftstats/config.json}. On a dedicated server the server's copy decides
 * who may edit what; the client's copy only affects client-side UI.
 */
public final class CraftStatsConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path configPath;
    private static ConfigData data = new ConfigData();

    private CraftStatsConfig() {}

    public static void init() {
        configPath = Platform.getConfigFolder().resolve("craftstats").resolve("config.json");
        load();
    }

    public static void load() {
        if (!Files.exists(configPath)) {
            data = new ConfigData();
            save();
            return;
        }
        try (Reader r = Files.newBufferedReader(configPath)) {
            ConfigData loaded = GSON.fromJson(r, ConfigData.class);
            data = loaded != null ? loaded : new ConfigData();
            data.normalize();
        } catch (IOException | JsonParseException e) {
            CraftStats.LOGGER.error("CraftStats: invalid config at {}, using defaults", configPath, e);
            data = new ConfigData();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer w = Files.newBufferedWriter(configPath)) {
                GSON.toJson(data, w);
            }
        } catch (IOException e) {
            CraftStats.LOGGER.error("CraftStats: failed to save config", e);
        }
    }

    public static ConfigData get() { return data; }

    public static class ConfigData {
        /** Only operators (and the single-player host) may edit. */
        public boolean requireOp = true;
        /** With requireOp off: also let survival players edit (otherwise creative only). */
        public boolean allowSurvival = false;
        /** mild, wild or chaos. */
        public String randomizeIntensity = "mild";

        public boolean enableBlockEditor = true;
        public boolean enableMobEditor = true;
        public boolean enableItemEditor = true;
        public boolean enablePlayerEditor = true;
        public boolean enableProjectileEditor = true;
        public boolean enableEnchantmentEditor = true;
        public boolean enableWorldEditor = true;
        public boolean enableRandomize = true;
        public boolean enablePresets = true;

        /** Namespaced IDs (e.g. "minecraft:wither") that can't be edited. */
        public List<String> blacklist = new ArrayList<>();
        /** Upper limit for the size scale of mobs. */
        public double maxScaleCap = 16.0;

        public boolean isEnabled(TargetType type) {
            return switch (type) {
                case MOB         -> enableMobEditor;
                case BLOCK       -> enableBlockEditor;
                case ITEM        -> enableItemEditor;
                case PLAYER      -> enablePlayerEditor;
                case PROJECTILE  -> enableProjectileEditor;
                case ENCHANTMENT -> enableEnchantmentEditor;
                case WORLD       -> enableWorldEditor;
            };
        }

        public boolean isBlacklisted(String id) {
            return blacklist.contains(id);
        }

        void normalize() {
            if (blacklist == null) blacklist = new ArrayList<>();
            if (randomizeIntensity == null) randomizeIntensity = "mild";
            if (!(maxScaleCap > 0)) maxScaleCap = 16.0;
        }
    }
}
