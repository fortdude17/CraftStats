package com.craftstats.fabric;

import com.craftstats.common.CraftStatsClient;
import com.craftstats.common.gui.BlockPosScreen;
import com.craftstats.common.gui.CraftStatsScreen;
import com.craftstats.common.gui.RandomizeScreen;
import com.craftstats.common.stats.MobStats;
import com.craftstats.common.stats.StatRegistry;
import com.craftstats.common.stats.TargetType;
import com.craftstats.common.util.ScreenOpener;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;

public class CraftStatsClientFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CraftStatsClient.init();
        registerScreenOpener();
    }

    private void registerScreenOpener() {
        ScreenOpener.setImpl(new ScreenOpener.Impl() {
            @Override
            public void openBlockEditor(Block block) {
                Minecraft.getInstance().setScreen(new CraftStatsScreen(block));
            }

            @Override
            public void openBlockEditorAt(String posKey, Block block, BlockPos pos) {
                Minecraft.getInstance().setScreen(new BlockPosScreen(posKey, block, pos));
            }

            @Override
            public void openMobEditor(LivingEntity entity) {
                Minecraft.getInstance().setScreen(new CraftStatsScreen(entity));
            }

            @Override
            public void openMobEditorById(String entityTypeId) {
                Minecraft.getInstance().setScreen(
                        new CraftStatsScreen(com.craftstats.common.stats.TargetType.MOB, entityTypeId));
            }

            @Override
            public void openItemEditor(String itemId) {
                CraftStatsScreen screen = new CraftStatsScreen();
                Minecraft.getInstance().setScreen(screen);
                screen.selectItemById(itemId);
            }

            @Override
            public void openPlayerEditor(Player player) {
                Minecraft.getInstance().setScreen(
                        new com.craftstats.common.gui.PlayerStatsScreen(
                                player.getUUID(), player.getName().getString()));
            }

            @Override
            public void openRandomizeBlock(Block block) {
                String id = BuiltInRegistries.BLOCK.getKey(block).toString();
                com.craftstats.common.stats.BlockStats stats = StatRegistry.getBlock(ResourceLocation.parse(id));
                if (stats == null) stats = new com.craftstats.common.stats.BlockStats();
                Minecraft.getInstance().setScreen(new RandomizeScreen(TargetType.BLOCK, id, stats));
            }

            @Override
            public void openRandomizeMob(LivingEntity entity) {
                String id = EntityType.getKey(entity.getType()).toString();
                MobStats stats = StatRegistry.getMob(ResourceLocation.parse(id));
                if (stats == null) stats = new MobStats();
                Minecraft.getInstance().setScreen(new RandomizeScreen(TargetType.MOB, id, stats));
            }
        });
    }
}
