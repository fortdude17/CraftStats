package com.craftstats.common;

import com.craftstats.common.network.CraftStatsNetwork;

public final class CraftStatsClient {

    public static void init() {
        CraftStatsNetwork.registerClientReceivers();
        CraftStats.LOGGER.info("CraftStats client initialised.");
    }
}
