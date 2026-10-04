package com.craftstats.common.stats;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * All active overrides. On a dedicated server the client holds a synced copy; in single
 * player the integrated server and the client share this one instance.
 */
public final class StatRegistry {

    private static final Map<ResourceLocation, MobStats>   MOBS            = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, BlockStats> BLOCKS          = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, ItemStats>  ITEMS           = new ConcurrentHashMap<>();
    private static final Map<UUID, PlayerStats>            PLAYERS         = new ConcurrentHashMap<>();
    private static final Map<UUID, MobStats>               MOB_INSTANCES   = new ConcurrentHashMap<>();
    private static final Map<String, BlockStats>           BLOCK_POSITIONS = new ConcurrentHashMap<>();

    /** Incremented on every change; used to invalidate caches derived from the overrides. */
    private static volatile int revision;

    private StatRegistry() {}

    public static void clear() {
        MOBS.clear(); BLOCKS.clear(); ITEMS.clear(); PLAYERS.clear();
        MOB_INSTANCES.clear(); BLOCK_POSITIONS.clear();
        changed();
    }

    public static int revision() { return revision; }
    public static void changed() { revision++; }

    public static String makePosKey(ResourceKey<Level> dim, BlockPos pos) {
        //#if MC >= 1.21.11
        //$ String dimId = dim.identifier().toString();
        //#else
        String dimId = dim.location().toString();
        //#endif
        return dimId + "|" + pos.getX() + "|" + pos.getY() + "|" + pos.getZ();
    }

    // ---- mobs --------------------------------------------------------------------------
    public static void setMob(ResourceLocation type, MobStats s) { MOBS.put(type, s); changed(); }
    public static MobStats getMob(ResourceLocation type)          { return MOBS.get(type); }
    public static void removeMob(ResourceLocation type)           { MOBS.remove(type); changed(); }
    public static Map<ResourceLocation, MobStats> allMobs()       { return MOBS; }

    public static void setMobInstance(UUID id, MobStats s)        { MOB_INSTANCES.put(id, s); changed(); }
    public static MobStats getMobInstance(UUID id)                { return MOB_INSTANCES.get(id); }
    public static void removeMobInstance(UUID id)                 { MOB_INSTANCES.remove(id); changed(); }
    public static Map<UUID, MobStats> allMobInstances()           { return MOB_INSTANCES; }

    /** The overrides that apply to a living entity: per-instance first, then per-type. Null for players. */
    public static MobStats forEntity(LivingEntity entity) {
        if (entity instanceof Player) return null;
        if (MOBS.isEmpty() && MOB_INSTANCES.isEmpty()) return null;
        MobStats s = MOB_INSTANCES.isEmpty() ? null : MOB_INSTANCES.get(entity.getUUID());
        return s != null ? s : MOBS.get(EntityType.getKey(entity.getType()));
    }

    // ---- blocks ------------------------------------------------------------------------
    public static void setBlock(ResourceLocation id, BlockStats s) { BLOCKS.put(id, s); changed(); }
    public static BlockStats getBlock(ResourceLocation id)         { return BLOCKS.get(id); }
    public static void removeBlock(ResourceLocation id)            { BLOCKS.remove(id); changed(); }
    public static Map<ResourceLocation, BlockStats> allBlocks()    { return BLOCKS; }

    public static void setBlockAt(String posKey, BlockStats s)     { BLOCK_POSITIONS.put(posKey, s); changed(); }
    public static BlockStats getBlockAt(String posKey)             { return BLOCK_POSITIONS.get(posKey); }
    public static void removeBlockAt(String posKey)                { BLOCK_POSITIONS.remove(posKey); changed(); }
    public static Map<String, BlockStats> allBlockPositions()      { return BLOCK_POSITIONS; }

    public static boolean hasBlockOverrides() { return !BLOCKS.isEmpty() || !BLOCK_POSITIONS.isEmpty(); }

    /** Type-level overrides for a block, or null. */
    public static BlockStats forBlock(Block block) {
        if (BLOCKS.isEmpty()) return null;
        return BLOCKS.get(BuiltInRegistries.BLOCK.getKey(block));
    }

    /**
     * Overrides for the block at a position: a per-position override (if it was made for the
     * block that is there now) takes priority over the type override.
     */
    public static BlockStats forBlockAt(Block block, BlockGetter getter, BlockPos pos) {
        if (BLOCKS.isEmpty() && BLOCK_POSITIONS.isEmpty()) return null;
        if (!BLOCK_POSITIONS.isEmpty() && getter instanceof Level level) {
            BlockStats s = BLOCK_POSITIONS.get(makePosKey(level.dimension(), pos));
            if (s != null && (s.block.isEmpty() || s.block.equals(BuiltInRegistries.BLOCK.getKey(block).toString())))
                return s;
        }
        return forBlock(block);
    }

    // ---- items -------------------------------------------------------------------------
    public static void setItem(ResourceLocation id, ItemStats s)   { ITEMS.put(id, s); changed(); }
    public static ItemStats getItem(ResourceLocation id)           { return ITEMS.get(id); }
    public static void removeItem(ResourceLocation id)             { ITEMS.remove(id); changed(); }
    public static Map<ResourceLocation, ItemStats> allItems()      { return ITEMS; }

    public static ItemStats forItem(Item item) {
        if (ITEMS.isEmpty()) return null;
        return ITEMS.get(BuiltInRegistries.ITEM.getKey(item));
    }

    // ---- players -----------------------------------------------------------------------
    public static void setPlayer(UUID id, PlayerStats s)           { PLAYERS.put(id, s); changed(); }
    public static PlayerStats getPlayer(UUID id)                   { return PLAYERS.get(id); }
    public static void removePlayer(UUID id)                       { PLAYERS.remove(id); changed(); }
    public static Map<UUID, PlayerStats> allPlayers()              { return PLAYERS; }

    public static PlayerStats forPlayer(Player player) {
        return PLAYERS.isEmpty() ? null : PLAYERS.get(player.getUUID());
    }
}
