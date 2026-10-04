package com.craftstats.neoforge;

import com.craftstats.common.CraftStatsClient;
import com.craftstats.common.gui.ConfigScreen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only setup; kept separate so a dedicated server never loads client classes. */
final class CraftStatsNeoForgeClient {

    private CraftStatsNeoForgeClient() {}

    static void init(ModContainer container) {
        CraftStatsClient.init();
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (IConfigScreenFactory) (mod, parent) -> new ConfigScreen(parent));
    }
}
