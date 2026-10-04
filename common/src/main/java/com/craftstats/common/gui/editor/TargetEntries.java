package com.craftstats.common.gui.editor;

import com.craftstats.common.client.ClientHooks;
import com.craftstats.common.stats.TargetType;
import com.craftstats.common.util.Compat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/** Builds (and caches) the lists of things that can be edited, with names and icons. */
public final class TargetEntries {

    private static final Map<TargetType, List<TargetList.Entry>> CACHE = new EnumMap<>(TargetType.class);
    private static Set<EntityType<?>> projectileTypes;

    private TargetEntries() {}

    public static List<TargetList.Entry> get(TargetType type) {
        if (type == TargetType.PLAYER) return players();          // changes while playing
        if (type == TargetType.ENCHANTMENT) return enchantments(); // depends on the world's data packs
        return CACHE.computeIfAbsent(type, TargetEntries::build);
    }

    private static List<TargetList.Entry> build(TargetType type) {
        List<TargetList.Entry> out = new ArrayList<>();
        switch (type) {
            case MOB -> {
                for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
                    if (t == EntityType.PLAYER || !DefaultAttributes.hasSupplier(t)) continue;
                    SpawnEggItem egg = SpawnEggItem.byId(t);
                    out.add(new TargetList.Entry(EntityType.getKey(t).toString(), t.getDescription().getString(),
                            egg != null ? new ItemStack(egg) : ItemStack.EMPTY));
                }
            }
            case BLOCK -> {
                for (Block b : BuiltInRegistries.BLOCK) {
                    if (b == Blocks.AIR || b == Blocks.CAVE_AIR || b == Blocks.VOID_AIR) continue;
                    Item item = b.asItem();
                    out.add(new TargetList.Entry(BuiltInRegistries.BLOCK.getKey(b).toString(), b.getName().getString(),
                            item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item)));
                }
            }
            case ITEM -> {
                for (Item i : BuiltInRegistries.ITEM) {
                    if (i == Items.AIR) continue;
                    ItemStack stack = new ItemStack(i);
                    out.add(new TargetList.Entry(BuiltInRegistries.ITEM.getKey(i).toString(), stack.getHoverName().getString(), stack));
                }
            }
            case PROJECTILE -> {
                for (EntityType<?> t : projectileTypes()) {
                    ResourceLocation id = EntityType.getKey(t);
                    out.add(new TargetList.Entry(id.toString(), t.getDescription().getString(), projectileIcon(id)));
                }
            }
            case WORLD -> out.add(new TargetList.Entry(TargetType.WORLD_KEY, "World Settings", new ItemStack(Items.GRASS_BLOCK)));
            default -> {}
        }
        out.sort(Comparator.comparing(TargetList.Entry::label, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    private static List<TargetList.Entry> players() {
        List<TargetList.Entry> out = new ArrayList<>();
        for (String s : ClientHooks.onlinePlayers()) {
            int i = s.indexOf(" | ");
            out.add(new TargetList.Entry(s.substring(0, i), s.substring(i + 3), new ItemStack(Items.PLAYER_HEAD)));
        }
        return out;
    }

    private static List<TargetList.Entry> enchantments() {
        List<TargetList.Entry> out = new ArrayList<>();
        Level level = Minecraft.getInstance().level;
        if (level == null) return out;
        Registry<Enchantment> reg = Compat.enchantments(level.registryAccess());
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        for (ResourceLocation id : reg.keySet()) {
            Enchantment e = Compat.registryValue(reg, id);
            out.add(new TargetList.Entry(id.toString(), e != null ? e.description().getString() : id.toString(), book));
        }
        out.sort(Comparator.comparing(TargetList.Entry::label, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    /**
     * Entity types whose entities are projectiles. Found by creating one of each
     * non-living entity type once (there is no registry flag for it).
     */
    public static Set<EntityType<?>> projectileTypes() {
        if (projectileTypes != null) return projectileTypes;
        Set<EntityType<?>> out = new HashSet<>();
        Level level = Minecraft.getInstance().level;
        for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
            if (t.getCategory() != MobCategory.MISC || DefaultAttributes.hasSupplier(t)) continue;
            if (level == null) continue;
            try {
                Entity e = Compat.createEntity(t, level);
                if (e instanceof Projectile) out.add(t);
                if (e != null) e.discard();
            } catch (Throwable ignored) {
                // Some modded entities can't be created this way; skip them.
            }
        }
        if (level != null) projectileTypes = out;
        return out;
    }

    private static ItemStack projectileIcon(ResourceLocation id) {
        String path = id.getPath();
        Item item = switch (path) {
            case "fireball", "small_fireball" -> Items.FIRE_CHARGE;
            case "dragon_fireball" -> Items.DRAGON_BREATH;
            case "wither_skull" -> Items.WITHER_SKELETON_SKULL;
            case "shulker_bullet" -> Items.SHULKER_SHELL;
            case "llama_spit" -> Items.LLAMA_SPAWN_EGG;
            case "fishing_bobber" -> Items.FISHING_ROD;
            case "potion", "splash_potion" -> Items.SPLASH_POTION;
            case "lingering_potion" -> Items.LINGERING_POTION;
            default -> BuiltInRegistries.ITEM.containsKey(id) ? Compat.registryValue(BuiltInRegistries.ITEM, id) : Items.ARROW;
        };
        return new ItemStack(item);
    }

    /** Forget cached lists (e.g. after joining another world). */
    public static void invalidate() {
        CACHE.clear();
        projectileTypes = null;
    }
}
