package com.craftstats.common.registry;

import com.craftstats.common.CraftStats;
import com.craftstats.common.item.ModItems;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;

public final class ModCreativeTab {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(CraftStats.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> CRAFTSTATS_TAB = TABS.register(
            "craftstats", () -> CreativeTabRegistry.create(
                    Component.translatable("itemGroup.craftstats"),
                    () -> ModItems.CRAFT_WAND.get().getDefaultInstance()
            )
    );

    public static void register() {
        TABS.register();
        CreativeTabRegistry.modify(CRAFTSTATS_TAB, (flags, output, canSeeOp) -> {
            output.accept(ModItems.CRAFT_WAND.get());
            output.accept(ModItems.PLAYER_STATS_BOOK.get());
        });
    }
}
