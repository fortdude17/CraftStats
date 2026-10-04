package com.craftstats.common.stats;

import com.craftstats.common.util.Compat;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Type-independent access to the overrides of one target, identified by a string key: a
 * registry id, a player UUID, or {@link TargetType#WORLD_KEY}.
 */
public final class StatTargets {

    private StatTargets() {}

    /** The stored overrides, or null. */
    public static Object get(TargetType type, String key) {
        return switch (type) {
            case MOB         -> withId(key, StatRegistry::getMob);
            case BLOCK       -> withId(key, StatRegistry::getBlock);
            case ITEM        -> withId(key, StatRegistry::getItem);
            case PROJECTILE  -> withId(key, StatRegistry::getProjectile);
            case ENCHANTMENT -> withId(key, StatRegistry::getEnchantment);
            case PLAYER      -> withUuid(key, StatRegistry::getPlayer);
            case WORLD       -> StatRegistry.world();
        };
    }

    /** Stores overrides and re-applies them to loaded mobs and players (server side). */
    public static void put(MinecraftServer server, TargetType type, String key, Object stats) {
        switch (type) {
            case MOB -> {
                ResourceLocation id = ResourceLocation.parse(key);
                StatRegistry.setMob(id, (MobStats) stats);
                if (server != null) StatApplier.refreshMobs(server, id);
            }
            case BLOCK       -> StatRegistry.setBlock(ResourceLocation.parse(key), (BlockStats) stats);
            case ITEM        -> StatRegistry.setItem(ResourceLocation.parse(key), (ItemStats) stats);
            case PROJECTILE  -> StatRegistry.setProjectile(ResourceLocation.parse(key), (ProjectileStats) stats);
            case ENCHANTMENT -> StatRegistry.setEnchantment(ResourceLocation.parse(key), (EnchantmentStats) stats);
            case PLAYER -> {
                UUID id = UUID.fromString(key);
                StatRegistry.setPlayer(id, (PlayerStats) stats);
                if (server != null) StatApplier.refreshPlayer(server, id);
            }
            case WORLD -> {
                StatRegistry.setWorld((WorldStats) stats);
                if (server != null) { StatApplier.refreshMobs(server, null); StatApplier.refreshAllPlayers(server); }
            }
        }
    }

    public static void remove(MinecraftServer server, TargetType type, String key) {
        switch (type) {
            case MOB -> {
                ResourceLocation id = ResourceLocation.parse(key);
                StatRegistry.removeMob(id);
                if (server != null) StatApplier.refreshMobs(server, id);
            }
            case BLOCK       -> StatRegistry.removeBlock(ResourceLocation.parse(key));
            case ITEM        -> StatRegistry.removeItem(ResourceLocation.parse(key));
            case PROJECTILE  -> StatRegistry.removeProjectile(ResourceLocation.parse(key));
            case ENCHANTMENT -> StatRegistry.removeEnchantment(ResourceLocation.parse(key));
            case PLAYER -> {
                UUID id = UUID.fromString(key);
                StatRegistry.removePlayer(id);
                if (server != null) StatApplier.refreshPlayer(server, id);
            }
            case WORLD -> {
                StatRegistry.removeWorld();
                if (server != null) { StatApplier.refreshMobs(server, null); StatApplier.refreshAllPlayers(server); }
            }
        }
    }

    /**
     * Checks that a registry-backed key names something that exists. Players and the world
     * are not checked here. Returns the normalized key, or null if unknown.
     */
    public static String validate(RegistryAccess access, TargetType type, String raw) {
        if (type == TargetType.WORLD) return TargetType.WORLD_KEY;
        if (type == TargetType.PLAYER) {
            try { return UUID.fromString(raw.trim()).toString(); } catch (IllegalArgumentException e) { return null; }
        }
        ResourceLocation id = ResourceLocation.tryParse(raw.trim());
        if (id == null) return null;
        Registry<?> registry = registry(access, type);
        return registry != null && registry.containsKey(id) ? id.toString() : null;
    }

    /** The registry a type's keys come from (null for players and the world). */
    public static Registry<?> registry(RegistryAccess access, TargetType type) {
        return switch (type) {
            case MOB, PROJECTILE -> BuiltInRegistries.ENTITY_TYPE;
            case BLOCK -> BuiltInRegistries.BLOCK;
            case ITEM -> BuiltInRegistries.ITEM;
            case ENCHANTMENT -> access == null ? null : Compat.enchantments(access);
            case PLAYER, WORLD -> null;
        };
    }

    /** All registry ids for a registry-backed type, sorted (empty for players and the world). */
    public static List<String> ids(RegistryAccess access, TargetType type) {
        Registry<?> registry = registry(access, type);
        List<String> out = new ArrayList<>();
        if (registry == null) return out;
        for (ResourceLocation id : registry.keySet()) out.add(id.toString());
        out.sort(null);
        return out;
    }

    /** Vanilla values of a target (for hints and randomizing). */
    public static Object vanilla(RegistryAccess access, TargetType type, String key) {
        ResourceLocation id = ResourceLocation.tryParse(key);
        return switch (type) {
            case MOB -> id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id)
                    ? VanillaStats.mob(Compat.registryValue(BuiltInRegistries.ENTITY_TYPE, id)) : new MobStats();
            case BLOCK -> id != null && BuiltInRegistries.BLOCK.containsKey(id)
                    ? VanillaStats.block(Compat.registryValue(BuiltInRegistries.BLOCK, id)) : new BlockStats();
            case ITEM -> id != null && BuiltInRegistries.ITEM.containsKey(id)
                    ? VanillaStats.item(Compat.registryValue(BuiltInRegistries.ITEM, id)) : new ItemStats();
            case ENCHANTMENT -> {
                Registry<Enchantment> reg = access == null ? null : Compat.enchantments(access);
                yield VanillaStats.enchantment(reg != null && id != null && reg.containsKey(id)
                        ? Compat.registryValue(reg, id) : null);
            }
            default -> VanillaStats.of(type);
        };
    }

    /** Number of stored overrides of a type (players and world count as entries too). */
    public static int count(TargetType type) {
        return switch (type) {
            case MOB -> StatRegistry.allMobs().size();
            case BLOCK -> StatRegistry.allBlocks().size();
            case ITEM -> StatRegistry.allItems().size();
            case PLAYER -> StatRegistry.allPlayers().size();
            case PROJECTILE -> StatRegistry.allProjectiles().size();
            case ENCHANTMENT -> StatRegistry.allEnchantments().size();
            case WORLD -> StatRegistry.world() != null ? 1 : 0;
        };
    }

    public static boolean isEntityType(String key) {
        ResourceLocation id = ResourceLocation.tryParse(key);
        return id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id)
                && Compat.registryValue(BuiltInRegistries.ENTITY_TYPE, id) != EntityType.PLAYER;
    }

    private static <T> T withId(String key, java.util.function.Function<ResourceLocation, T> getter) {
        ResourceLocation id = ResourceLocation.tryParse(key);
        return id == null ? null : getter.apply(id);
    }

    private static <T> T withUuid(String key, java.util.function.Function<UUID, T> getter) {
        try { return getter.apply(UUID.fromString(key)); } catch (IllegalArgumentException e) { return null; }
    }
}
