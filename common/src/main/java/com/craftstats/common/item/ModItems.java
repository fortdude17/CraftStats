package com.craftstats.common.item;

import com.craftstats.common.CraftStats;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(CraftStats.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<Item> CRAFT_WAND =
            ITEMS.register("craft_wand",
                    () -> new CraftWandItem(new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> PLAYER_STATS_BOOK =
            ITEMS.register("player_stats_book",
                    () -> new PlayerStatsBookItem(new Item.Properties().stacksTo(1)));

    public static void register() {
        ITEMS.register();
    }
}
