package com.craftstats.common.util;

import com.craftstats.common.CraftStats;
import com.craftstats.common.stats.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** Saves overrides per world in {@code <world>/craftstats/stats.json}. */
public final class StatPersistence {

    private static MinecraftServer server;

    private StatPersistence() {}

    public static void init(MinecraftServer srv) {
        server = srv;
        StatRegistry.clear();
        load();
    }

    public static void shutdown() {
        save();
        server = null;
        StatRegistry.clear();
    }

    public static void save() {
        if (server == null) return;
        Path file = getFile();
        if (file == null) return;
        try {
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, StatSchema.GSON.toJson(snapshot()), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            CraftStats.LOGGER.error("CraftStats: failed to save {}", file, e);
        }
    }

    /** The whole registry as JSON. Also used to sync clients. */
    public static JsonObject snapshot() {
        JsonObject root = new JsonObject();
        root.addProperty("v", StatSchema.CURRENT);
        root.add("mobs",            write(StatRegistry.allMobs()));
        root.add("blocks",          write(StatRegistry.allBlocks()));
        root.add("items",           write(StatRegistry.allItems()));
        root.add("players",         write(StatRegistry.allPlayers()));
        root.add("mob_instances",   write(StatRegistry.allMobInstances()));
        root.add("block_positions", write(StatRegistry.allBlockPositions()));
        root.add("projectiles",     write(StatRegistry.allProjectiles()));
        root.add("enchantments",    write(StatRegistry.allEnchantments()));
        if (StatRegistry.world() != null) root.add("world", StatSchema.GSON_COMPACT.toJsonTree(StatRegistry.world()));
        return root;
    }

    /** Replaces the registry contents with a snapshot. Bad entries are skipped and logged. */
    public static void restore(JsonObject root) {
        StatRegistry.clear();
        read(root, "mobs",            TargetType.MOB,    ResourceLocation::parse, StatRegistry::setMob);
        read(root, "blocks",          TargetType.BLOCK,  ResourceLocation::parse, StatRegistry::setBlock);
        read(root, "items",           TargetType.ITEM,   ResourceLocation::parse, StatRegistry::setItem);
        read(root, "players",         TargetType.PLAYER, UUID::fromString,        StatRegistry::setPlayer);
        read(root, "mob_instances",   TargetType.MOB,    UUID::fromString,        StatRegistry::setMobInstance);
        read(root, "block_positions", TargetType.BLOCK,  Function.identity(),     StatRegistry::setBlockAt);
        read(root, "projectiles",     TargetType.PROJECTILE,  ResourceLocation::parse, StatRegistry::setProjectile);
        read(root, "enchantments",    TargetType.ENCHANTMENT, ResourceLocation::parse, StatRegistry::setEnchantment);
        if (root.has("world") && root.get("world").isJsonObject()) {
            try {
                StatRegistry.setWorld(StatSchema.parse(TargetType.WORLD, root.get("world")));
            } catch (Exception ex) {
                CraftStats.LOGGER.warn("CraftStats: skipping invalid world settings: {}", ex.toString());
            }
        }
    }

    private static JsonObject write(Map<?, ?> map) {
        JsonObject obj = new JsonObject();
        map.forEach((k, v) -> obj.add(k.toString(), StatSchema.GSON_COMPACT.toJsonTree(v)));
        return obj;
    }

    private static <K, V> void read(JsonObject root, String section, TargetType type,
                                    Function<String, K> key, BiConsumer<K, V> sink) {
        if (!root.has(section) || !root.get(section).isJsonObject()) return;
        for (Map.Entry<String, JsonElement> e : root.getAsJsonObject(section).entrySet()) {
            try {
                JsonElement value = e.getValue();
                // CraftStats 1.0 stored each entry as a JSON string.
                if (value.isJsonPrimitive()) value = JsonParser.parseString(value.getAsString());
                sink.accept(key.apply(e.getKey()), StatSchema.parse(type, value));
            } catch (Exception ex) {
                CraftStats.LOGGER.warn("CraftStats: skipping invalid {} entry '{}': {}", section, e.getKey(), ex.toString());
            }
        }
    }

    private static void load() {
        Path file = getFile();
        if (file == null || !Files.exists(file)) return;
        try {
            restore(JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject());
            CraftStats.LOGGER.info("CraftStats: loaded {} mob, {} block, {} item, {} player, {} projectile, {} enchantment, "
                            + "{} mob-instance and {} block-position override(s){}.",
                    StatRegistry.allMobs().size(), StatRegistry.allBlocks().size(),
                    StatRegistry.allItems().size(), StatRegistry.allPlayers().size(),
                    StatRegistry.allProjectiles().size(), StatRegistry.allEnchantments().size(),
                    StatRegistry.allMobInstances().size(), StatRegistry.allBlockPositions().size(),
                    StatRegistry.world() != null ? " and world settings" : "");
        } catch (Exception e) {
            // Keep the unreadable file so the user doesn't lose it on the next save.
            Path backup = file.resolveSibling("stats.json.broken-" + System.currentTimeMillis());
            try { Files.copy(file, backup); } catch (IOException ignored) {}
            CraftStats.LOGGER.error("CraftStats: could not read {} (backed up to {})", file, backup.getFileName(), e);
        }
    }

    private static Path getFile() {
        if (server == null) return null;
        try {
            Path dir = server.getWorldPath(LevelResource.ROOT).resolve("craftstats");
            Files.createDirectories(dir);
            return dir.resolve("stats.json");
        } catch (IOException e) {
            CraftStats.LOGGER.error("CraftStats: could not create save directory", e);
            return null;
        }
    }
}
