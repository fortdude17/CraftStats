package com.craftstats.common.preset;

import com.craftstats.common.stats.StatSchema;
import com.craftstats.common.stats.TargetType;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** A named, reusable set of stats for one target type. */
public class Preset {

    public String     name;
    public TargetType targetType;
    public long       seed;
    public boolean    readonly;
    /** A MobStats, BlockStats, ItemStats or PlayerStats matching {@link #targetType}. */
    public Object     stats;

    public Preset(String name, TargetType type, Object stats) {
        this.name = name;
        this.targetType = type;
        this.stats = stats;
    }

    /** A copy of the stats, safe to modify. */
    public Object copyStats() {
        return StatSchema.parse(targetType, StatSchema.GSON_COMPACT.toJsonTree(stats));
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("name", name);
        o.addProperty("target_type", targetType.name().toLowerCase());
        if (seed != 0) o.addProperty("seed", seed);
        o.add("stats", StatSchema.GSON_COMPACT.toJsonTree(stats));
        return o;
    }

    /** Parses a preset; the stats are migrated and typed. Throws on invalid input. */
    public static Preset fromJson(JsonElement json) {
        if (json == null || !json.isJsonObject()) throw new IllegalArgumentException("not a JSON object");
        JsonObject o = json.getAsJsonObject();
        if (!o.has("name") || !o.has("target_type") || !o.has("stats"))
            throw new IllegalArgumentException("needs name, target_type and stats");
        TargetType type = TargetType.valueOf(o.get("target_type").getAsString().toUpperCase());
        Preset p = new Preset(o.get("name").getAsString(), type, StatSchema.parse(type, o.get("stats")));
        if (o.has("seed")) p.seed = o.get("seed").getAsLong();
        return p;
    }
}
