package com.craftstats.common.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * JSON (de)serialization for stat objects, including migration of data written by
 * CraftStats 1.0.x. That version used -1 (and a few other magic defaults) to mean
 * "vanilla"; current data uses a missing value instead and carries a {@code "v"} field.
 */
public final class StatSchema {

    public static final int CURRENT = 2;

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Gson GSON_COMPACT = new GsonBuilder().create();

    private StatSchema() {}

    public static <T> T copy(T stats, Class<T> type) {
        return GSON_COMPACT.fromJson(GSON_COMPACT.toJsonTree(stats), type);
    }

    public static Class<?> classFor(TargetType type) {
        return switch (type) {
            case MOB    -> MobStats.class;
            case BLOCK  -> BlockStats.class;
            case ITEM   -> ItemStats.class;
            case PLAYER -> PlayerStats.class;
        };
    }

    /** Parses stats of the given type, migrating legacy data. Never returns null. */
    @SuppressWarnings("unchecked")
    public static <T> T parse(TargetType type, JsonElement json) {
        if (json == null || !json.isJsonObject())
            throw new IllegalArgumentException("Expected a JSON object for " + type.displayName() + " stats");
        JsonObject obj = json.getAsJsonObject().deepCopy();
        if (!obj.has("v")) migrateV1(type, obj);
        return (T) GSON_COMPACT.fromJson(obj, classFor(type));
    }

    public static <T> T parse(TargetType type, String json) {
        return parse(type, JsonParser.parseString(json));
    }

    public static MobStats mob(String json)       { return parse(TargetType.MOB, json); }
    public static BlockStats block(String json)   { return parse(TargetType.BLOCK, json); }
    public static ItemStats item(String json)     { return parse(TargetType.ITEM, json); }
    public static PlayerStats player(String json) { return parse(TargetType.PLAYER, json); }

    public static String toJson(Object stats)        { return GSON_COMPACT.toJson(stats); }
    public static String toPrettyJson(Object stats)  { return GSON.toJson(stats); }

    private static void migrateV1(TargetType type, JsonObject o) {
        switch (type) {
            case MOB -> {
                for (String k : new String[]{"max_health", "attack_damage", "armor", "knockback_resist",
                        "move_speed", "follow_range", "size_scale"})
                    removeIfNegative(o, k);
                o.remove("jump_force"); // was never applied
                removeIf(o, "xp_reward", 0); // 0 meant "vanilla"
            }
            case BLOCK -> {
                for (String k : new String[]{"hardness", "blast_resistance", "slipperiness", "light_emission", "drop_xp"})
                    removeIfNegative(o, k);
            }
            case ITEM -> {
                // Old defaults that were always written, whether or not the user changed them.
                removeIf(o, "attack_damage", 1.0);
                removeIf(o, "attack_speed", 4.0);
                removeIf(o, "enchantability", 0);
                removeIf(o, "stack_size", 64);
                removeIf(o, "mining_speed", 1.0);
                removeIfNegative(o, "max_durability");
                removeIf(o, "max_durability", 0);
                // Food values were never applied; drop them so nothing changes unexpectedly.
                for (String k : new String[]{"is_food", "nutrition", "saturation", "eat_duration", "always_edible", "on_eat_effect"})
                    o.remove(k);
            }
            case PLAYER -> {
                removeIf(o, "burning_time", 8.0);          // attribute is a multiplier (vanilla 1.0)
                removeIf(o, "sweeping_damage_ratio", 1.0); // vanilla is 0.0
            }
        }
    }

    private static void removeIfNegative(JsonObject o, String key) {
        JsonElement e = o.get(key);
        if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() && e.getAsDouble() < 0)
            o.remove(key);
    }

    private static void removeIf(JsonObject o, String key, double value) {
        JsonElement e = o.get(key);
        if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() && e.getAsDouble() == value)
            o.remove(key);
    }
}
