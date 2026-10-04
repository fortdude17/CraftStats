package com.craftstats.common;

import com.craftstats.common.client.ClientHooks;
import com.craftstats.common.client.ClientSmokeTest;
import com.craftstats.common.network.CraftStatsNetwork;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.common.PlayerEvent;
import com.craftstats.common.stats.StatRegistry;
import com.craftstats.common.util.Compat;

public final class CraftStatsClient {

    private CraftStatsClient() {}

    public static void init() {
        CraftStatsNetwork.registerClient(ClientHooks::onSync);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> ClientHooks.onDisconnect());
        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player ->
                StatRegistry.trackEnchantments(Compat.enchantments(player.level().registryAccess())));
        PlayerEvent.ATTACK_ENTITY.register(ClientHooks::onAttackEntity);
        ClientSmokeTest.register();
        CraftStats.LOGGER.info("CraftStats client initialised.");
    }
}
