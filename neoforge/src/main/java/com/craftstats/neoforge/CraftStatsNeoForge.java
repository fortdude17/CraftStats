package com.craftstats.neoforge;

import com.craftstats.common.CraftStats;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(CraftStats.MOD_ID)
public class CraftStatsNeoForge {

    public CraftStatsNeoForge(IEventBus modBus, ModContainer container) {
        CraftStats.init();
        if (Platform.getEnvironment() == Env.CLIENT) CraftStatsNeoForgeClient.init(container);
    }
}
