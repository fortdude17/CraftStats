package com.craftstats.fabric;

import com.craftstats.common.CraftStats;
import net.fabricmc.api.ModInitializer;

public class CraftStatsFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        CraftStats.init();
    }
}
