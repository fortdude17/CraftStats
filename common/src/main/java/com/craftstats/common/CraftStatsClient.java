package com.craftstats.common;

import com.craftstats.common.client.ClientHooks;
import com.craftstats.common.network.CraftStatsNetwork;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.common.PlayerEvent;

public final class CraftStatsClient {

    private CraftStatsClient() {}

    public static void init() {
        CraftStatsNetwork.registerClient(ClientHooks::onSync);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> ClientHooks.onDisconnect());
        PlayerEvent.ATTACK_ENTITY.register(ClientHooks::onAttackEntity);
        CraftStats.LOGGER.info("CraftStats client initialised.");
    }
}
