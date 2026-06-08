package com.craftstats.common.util;

import com.craftstats.common.stats.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class JsonUtil {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Gson GSON_COMPACT = new GsonBuilder().create();

    private JsonUtil() {}

    public static String serializeMobStats(LivingEntity entity) {
        MobStats stats = new MobStats();
        stats.maxHealth = entity.getMaxHealth();
        var attrDamage = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attrDamage != null) stats.attackDamage = attrDamage.getValue();
        var attrArmor = entity.getAttribute(Attributes.ARMOR);
        if (attrArmor != null) stats.armor = attrArmor.getValue();
        var attrSpeed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attrSpeed != null) stats.moveSpeed = attrSpeed.getValue();

        JsonObject root = new JsonObject();
        root.addProperty("type", "mob");
        root.addProperty("entity_type", entity.getType().toString());
        root.add("stats", GSON.toJsonTree(stats));
        return GSON.toJson(root);
    }

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    public static String toCompactJson(Object obj) {
        return GSON_COMPACT.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    public static MobStats mobStatsFromJson(String json) {
        return GSON.fromJson(json, MobStats.class);
    }

    public static BlockStats blockStatsFromJson(String json) {
        return GSON.fromJson(json, BlockStats.class);
    }

    public static ItemStats itemStatsFromJson(String json) {
        return GSON.fromJson(json, ItemStats.class);
    }

    public static PlayerStats playerStatsFromJson(String json) {
        return GSON.fromJson(json, PlayerStats.class);
    }
}
