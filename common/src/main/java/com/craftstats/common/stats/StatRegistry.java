package com.craftstats.common.stats;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StatRegistry {

    private static final Map<ResourceLocation, MobStats>    MOB_OVERRIDES       = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, BlockStats>   BLOCK_OVERRIDES     = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, ItemStats>    ITEM_OVERRIDES      = new ConcurrentHashMap<>();
    private static final Map<UUID,             PlayerStats>  PLAYER_OVERRIDES    = new ConcurrentHashMap<>();

    private static final Map<UUID,             MobStats>     MOB_UUID_OVERRIDES  = new ConcurrentHashMap<>();

    private static final Map<String,           BlockStats>   BLOCK_POS_OVERRIDES = new ConcurrentHashMap<>();

    public static void init() {
        MOB_OVERRIDES.clear();
        BLOCK_OVERRIDES.clear();
        ITEM_OVERRIDES.clear();
        PLAYER_OVERRIDES.clear();
        MOB_UUID_OVERRIDES.clear();
        BLOCK_POS_OVERRIDES.clear();
    }

    public static String makePosKey(ResourceKey<Level> dim, BlockPos pos) {
        return dim.location() + "|" + pos.getX() + "|" + pos.getY() + "|" + pos.getZ();
    }

    public static void setMob(ResourceLocation type, MobStats stats)  { MOB_OVERRIDES.put(type, stats); }
    public static MobStats getMob(ResourceLocation type)               { return MOB_OVERRIDES.get(type); }
    public static void removeMob(ResourceLocation type)                { MOB_OVERRIDES.remove(type); }
    public static Map<ResourceLocation, MobStats> allMobs()            { return MOB_OVERRIDES; }

    public static void setBlock(ResourceLocation id, BlockStats s)     { BLOCK_OVERRIDES.put(id, s); }
    public static BlockStats getBlock(ResourceLocation id)             { return BLOCK_OVERRIDES.get(id); }
    public static void removeBlock(ResourceLocation id)                { BLOCK_OVERRIDES.remove(id); }
    public static Map<ResourceLocation, BlockStats> allBlocks()        { return BLOCK_OVERRIDES; }

    public static void setItem(ResourceLocation id, ItemStats s)       { ITEM_OVERRIDES.put(id, s); }
    public static ItemStats getItem(ResourceLocation id)               { return ITEM_OVERRIDES.get(id); }
    public static void removeItem(ResourceLocation id)                 { ITEM_OVERRIDES.remove(id); }
    public static Map<ResourceLocation, ItemStats> allItems()          { return ITEM_OVERRIDES; }

    public static void setPlayer(UUID uuid, PlayerStats s)             { PLAYER_OVERRIDES.put(uuid, s); }
    public static PlayerStats getPlayer(UUID uuid)                     { return PLAYER_OVERRIDES.get(uuid); }
    public static void removePlayer(UUID uuid)                         { PLAYER_OVERRIDES.remove(uuid); }
    public static Map<UUID, PlayerStats> allPlayers()                  { return PLAYER_OVERRIDES; }

    public static void setMobUuid(UUID uuid, MobStats stats)  { MOB_UUID_OVERRIDES.put(uuid, stats); }
    public static MobStats getMobUuid(UUID uuid)               { return MOB_UUID_OVERRIDES.get(uuid); }
    public static void removeMobUuid(UUID uuid)                { MOB_UUID_OVERRIDES.remove(uuid); }
    public static Map<UUID, MobStats> allMobUuids()            { return MOB_UUID_OVERRIDES; }

    public static void setBlockAt(String posKey, BlockStats s)         { BLOCK_POS_OVERRIDES.put(posKey, s); }
    public static BlockStats getBlockAt(String posKey)                 { return BLOCK_POS_OVERRIDES.get(posKey); }
    public static void removeBlockAt(String posKey)                    { BLOCK_POS_OVERRIDES.remove(posKey); }
    public static Map<String, BlockStats> allBlockPositions()          { return BLOCK_POS_OVERRIDES; }
}
