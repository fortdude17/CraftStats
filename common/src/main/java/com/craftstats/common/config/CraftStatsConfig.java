package com.craftstats.common.config;

import com.craftstats.common.CraftStats;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.architectury.platform.Platform;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CraftStatsConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path configPath;
    private static ConfigData data = new ConfigData();

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
        } catch (IOException e) {
            CraftStats.LOGGER.error("Failed to load config", e);
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
            CraftStats.LOGGER.error("Failed to save config", e);
        }
    }

    public static ConfigData get() { return data; }

    public static class ConfigData {
        public boolean requireOp = true;
        public boolean allowSurvival = false;
        public String randomizeIntensity = "mild";
        public long randomizeSeedOverride = -1;

        public boolean enableBlockEditor = true;
        public boolean enableMobEditor = true;
        public boolean enableItemEditor = true;
        public boolean enablePlayerEditor = true;
        public boolean enableRandomize = true;
        public boolean enablePropagate = true;
        public boolean enablePresets = true;

        public List<String> blacklist = new ArrayList<>();
        public double maxScaleCap = 100.0;
        public boolean persistChangesOnRestart = false;
        public boolean confirmPropagate = true;
    }
}
