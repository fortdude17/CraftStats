package com.craftstats.fabric;

import com.craftstats.common.CraftStatsClient;
import net.fabricmc.api.ClientModInitializer;

public class CraftStatsClientFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CraftStatsClient.init();
    }
}
