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

import com.craftstats.common.util.Compat;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
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
    private static final Map<ResourceLocation, ProjectileStats>  PROJECTILES  = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, EnchantmentStats> ENCHANTMENTS = new ConcurrentHashMap<>();
    private static volatile WorldStats world;

    /** Incremented on every change; used to invalidate caches derived from the overrides. */
    private static volatile int revision;

    /**
     * Identity-keyed views of the type maps, rebuilt after changes. Block lookups happen in hot
     * paths (lighting, collision), so they must not hash ids every call.
     */
    private record Caches(int revision, Map<Block, BlockStats> blocks, Map<EntityType<?>, MobStats> mobs,
                          Map<Item, ItemStats> items, Map<EntityType<?>, ProjectileStats> projectiles) {}
    private static volatile Caches caches = new Caches(-1, Map.of(), Map.of(), Map.of(), Map.of());

    private static Caches caches() {
        Caches c = caches;
        int rev = revision;
        if (c.revision() == rev) return c;
        Map<Block, BlockStats> blocks = new IdentityHashMap<>();
        // Defaulted registries return air/pig for unknown ids, hence the containsKey checks.
        BLOCKS.forEach((id, s) -> { if (BuiltInRegistries.BLOCK.containsKey(id)) blocks.put(Compat.registryValue(BuiltInRegistries.BLOCK, id), s); });
        Map<EntityType<?>, MobStats> mobs = new IdentityHashMap<>();
        MOBS.forEach((id, s) -> { if (BuiltInRegistries.ENTITY_TYPE.containsKey(id)) mobs.put(Compat.registryValue(BuiltInRegistries.ENTITY_TYPE, id), s); });
        Map<Item, ItemStats> items = new IdentityHashMap<>();
        ITEMS.forEach((id, s) -> { if (BuiltInRegistries.ITEM.containsKey(id)) items.put(Compat.registryValue(BuiltInRegistries.ITEM, id), s); });
        Map<EntityType<?>, ProjectileStats> projectiles = new IdentityHashMap<>();
        PROJECTILES.forEach((id, s) -> { if (BuiltInRegistries.ENTITY_TYPE.containsKey(id)) projectiles.put(Compat.registryValue(BuiltInRegistries.ENTITY_TYPE, id), s); });
        c = new Caches(rev, blocks, mobs, items, projectiles);
        caches = c;
        return c;
    }

    private StatRegistry() {}

    public static void clear() {
        MOBS.clear(); BLOCKS.clear(); ITEMS.clear(); PLAYERS.clear();
        MOB_INSTANCES.clear(); BLOCK_POSITIONS.clear();
        PROJECTILES.clear(); ENCHANTMENTS.clear(); world = null;
        changed();
    }

    public static int revision() { return revision; }
    public static void changed() { revision++; }

    public static String makePosKey(ResourceKey<Level> dim, BlockPos pos) {
        String dimId = Compat.keyId(dim).toString();
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
        return s != null || MOBS.isEmpty() ? s : caches().mobs().get(entity.getType());
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
        return BLOCKS.isEmpty() ? null : caches().blocks().get(block);
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
        return ITEMS.isEmpty() ? null : caches().items().get(item);
    }

    // ---- players -----------------------------------------------------------------------
    public static void setPlayer(UUID id, PlayerStats s)           { PLAYERS.put(id, s); changed(); }
    public static PlayerStats getPlayer(UUID id)                   { return PLAYERS.get(id); }
    public static void removePlayer(UUID id)                       { PLAYERS.remove(id); changed(); }
    public static Map<UUID, PlayerStats> allPlayers()              { return PLAYERS; }

    public static PlayerStats forPlayer(Player player) {
        return PLAYERS.isEmpty() ? null : PLAYERS.get(player.getUUID());
    }

    // ---- projectiles -------------------------------------------------------------------
    public static void setProjectile(ResourceLocation id, ProjectileStats s) { PROJECTILES.put(id, s); changed(); }
    public static ProjectileStats getProjectile(ResourceLocation id)         { return PROJECTILES.get(id); }
    public static void removeProjectile(ResourceLocation id)                 { PROJECTILES.remove(id); changed(); }
    public static Map<ResourceLocation, ProjectileStats> allProjectiles()    { return PROJECTILES; }

    /** Overrides for a projectile entity (by type), or null. */
    public static ProjectileStats forProjectile(Entity entity) {
        return PROJECTILES.isEmpty() ? null : caches().projectiles().get(entity.getType());
    }

    // ---- enchantments ------------------------------------------------------------------
    public static void setEnchantment(ResourceLocation id, EnchantmentStats s) { ENCHANTMENTS.put(id, s); changed(); }
    public static EnchantmentStats getEnchantment(ResourceLocation id)         { return ENCHANTMENTS.get(id); }
    public static void removeEnchantment(ResourceLocation id)                  { ENCHANTMENTS.remove(id); changed(); }
    public static Map<ResourceLocation, EnchantmentStats> allEnchantments()    { return ENCHANTMENTS; }
    public static boolean hasEnchantmentOverrides()                           { return !ENCHANTMENTS.isEmpty(); }

    public static EnchantmentStats forEnchantment(Holder<Enchantment> holder) {
        if (ENCHANTMENTS.isEmpty()) return null;
        return holder.unwrapKey().map(k -> ENCHANTMENTS.get(Compat.keyId(k))).orElse(null);
    }

    /**
     * Enchantments are data-driven and the Enchantment object has no back-reference to its
     * id, so look it up in the registries we know about (server and client have their own).
     */
    public static EnchantmentStats forEnchantment(Enchantment enchantment) {
        if (ENCHANTMENTS.isEmpty()) return null;
        for (Registry<Enchantment> registry : ENCHANTMENT_REGISTRIES) {
            ResourceLocation id = registry.getKey(enchantment);
            if (id != null) return ENCHANTMENTS.get(id);
        }
        return null;
    }

    private static final List<Registry<Enchantment>> ENCHANTMENT_REGISTRIES = new CopyOnWriteArrayList<>();

    /** Called when a server starts or a client joins a world. */
    public static void trackEnchantments(Registry<Enchantment> registry) {
        if (registry != null && !ENCHANTMENT_REGISTRIES.contains(registry)) ENCHANTMENT_REGISTRIES.add(0, registry);
        while (ENCHANTMENT_REGISTRIES.size() > 4) ENCHANTMENT_REGISTRIES.remove(ENCHANTMENT_REGISTRIES.size() - 1);
    }

    // ---- world -------------------------------------------------------------------------
    public static void setWorld(WorldStats s) { world = s; changed(); }
    public static void removeWorld()          { world = null; changed(); }
    /** World-wide rules, or null when nothing is changed. */
    public static WorldStats world()          { return world; }
}
