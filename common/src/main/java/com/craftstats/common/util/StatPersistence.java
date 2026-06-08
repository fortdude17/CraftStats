package com.craftstats.common.util;

import com.craftstats.common.CraftStats;
import com.craftstats.common.stats.*;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class StatPersistence {

    private static MinecraftServer server;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private StatPersistence() {}

    public static void init(MinecraftServer srv) {
        server = srv;
        StatRegistry.init();
        load();
    }

    public static void save() {
        if (server == null) return;
        Path file = getFile();
        if (file == null) return;
        try {
            JsonObject root = new JsonObject();

            JsonObject mobs = new JsonObject();
            for (Map.Entry<ResourceLocation, MobStats> e : StatRegistry.allMobs().entrySet())
                mobs.addProperty(e.getKey().toString(), JsonUtil.toCompactJson(e.getValue()));
            root.add("mobs", mobs);

            JsonObject blocks = new JsonObject();
            for (Map.Entry<ResourceLocation, BlockStats> e : StatRegistry.allBlocks().entrySet())
                blocks.addProperty(e.getKey().toString(), JsonUtil.toCompactJson(e.getValue()));
            root.add("blocks", blocks);

            JsonObject items = new JsonObject();
            for (Map.Entry<ResourceLocation, ItemStats> e : StatRegistry.allItems().entrySet())
                items.addProperty(e.getKey().toString(), JsonUtil.toCompactJson(e.getValue()));
            root.add("items", items);

            JsonObject players = new JsonObject();
            for (Map.Entry<UUID, PlayerStats> e : StatRegistry.allPlayers().entrySet())
                players.addProperty(e.getKey().toString(), JsonUtil.toCompactJson(e.getValue()));
            root.add("players", players);

            JsonObject mobInstances = new JsonObject();
            for (Map.Entry<UUID, MobStats> e : StatRegistry.allMobUuids().entrySet())
                mobInstances.addProperty(e.getKey().toString(), JsonUtil.toCompactJson(e.getValue()));
            root.add("mob_instances", mobInstances);

            JsonObject blockPos = new JsonObject();
            for (Map.Entry<String, BlockStats> e : StatRegistry.allBlockPositions().entrySet())
                blockPos.addProperty(e.getKey(), JsonUtil.toCompactJson(e.getValue()));
            root.add("block_positions", blockPos);

            Files.writeString(file, GSON.toJson(root));
        } catch (IOException e) {
            CraftStats.LOGGER.error("CraftStats: save failed: {}", e.getMessage());
        }
    }

    private static void load() {
        Path file = getFile();
        if (file == null || !Files.exists(file)) return;
        try {
            String text = Files.readString(file);
            JsonObject root = JsonParser.parseString(text).getAsJsonObject();
            if (root.has("mobs"))
                for (var e : root.getAsJsonObject("mobs").entrySet())
                    StatRegistry.setMob(ResourceLocation.parse(e.getKey()),
                            JsonUtil.mobStatsFromJson(e.getValue().getAsString()));
            if (root.has("blocks"))
                for (var e : root.getAsJsonObject("blocks").entrySet())
                    StatRegistry.setBlock(ResourceLocation.parse(e.getKey()),
                            JsonUtil.blockStatsFromJson(e.getValue().getAsString()));
            if (root.has("items"))
                for (var e : root.getAsJsonObject("items").entrySet())
                    StatRegistry.setItem(ResourceLocation.parse(e.getKey()),
                            JsonUtil.itemStatsFromJson(e.getValue().getAsString()));
            if (root.has("players"))
                for (var e : root.getAsJsonObject("players").entrySet())
                    StatRegistry.setPlayer(UUID.fromString(e.getKey()),
                            JsonUtil.playerStatsFromJson(e.getValue().getAsString()));
            if (root.has("mob_instances"))
                for (var e : root.getAsJsonObject("mob_instances").entrySet())
                    StatRegistry.setMobUuid(UUID.fromString(e.getKey()),
                            JsonUtil.mobStatsFromJson(e.getValue().getAsString()));
            if (root.has("block_positions"))
                for (var e : root.getAsJsonObject("block_positions").entrySet())
                    StatRegistry.setBlockAt(e.getKey(),
                            JsonUtil.blockStatsFromJson(e.getValue().getAsString()));
            CraftStats.LOGGER.info("CraftStats: loaded {} mob, {} block, {} item, {} player, {} block-pos override(s).",
                    StatRegistry.allMobs().size(), StatRegistry.allBlocks().size(),
                    StatRegistry.allItems().size(), StatRegistry.allPlayers().size(),
                    StatRegistry.allBlockPositions().size());
        } catch (Exception e) {
            CraftStats.LOGGER.error("CraftStats: load failed from {}: {}", file, e.getMessage());
        }
    }

    private static Path getFile() {
        if (server == null) return null;
        try {
            Path dir = server.getWorldPath(LevelResource.ROOT).resolve("craftstats");
            Files.createDirectories(dir);
            return dir.resolve("stats.json");
        } catch (IOException e) {
            CraftStats.LOGGER.error("CraftStats: could not create save directory: {}", e.getMessage());
            return null;
        }
    }
}
