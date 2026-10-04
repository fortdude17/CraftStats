package com.craftstats.common.item;

import com.craftstats.common.CraftStats;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
//#if MC >= 1.21.2
//$ import net.minecraft.resources.ResourceKey;
//$ import net.minecraft.resources.ResourceLocation;
//#endif

public final class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(CraftStats.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<Item> CRAFT_WAND =
            ITEMS.register("craft_wand", () -> new CraftWandItem(properties("craft_wand").stacksTo(1)));

    public static final RegistrySupplier<Item> PLAYER_STATS_BOOK =
            ITEMS.register("player_stats_book", () -> new PlayerStatsBookItem(properties("player_stats_book").stacksTo(1)));

    private ModItems() {}

    private static Item.Properties properties(String name) {
        Item.Properties props = new Item.Properties();
        //#if MC >= 1.21.2
        //$ props = props.setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(CraftStats.MOD_ID, name)));
        //#endif
        return props;
    }

    public static void register() {
        ITEMS.register();
    }
}
