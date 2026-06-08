package com.craftstats.fabric.config;

import com.craftstats.common.config.CraftStatsConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

public class CraftStatsModMenuConfig implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new CraftStatsConfigScreen(parent);
    }
}
