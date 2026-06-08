package com.craftstats.neoforge;

import com.craftstats.common.CraftStats;
import com.craftstats.common.CraftStatsClient;
import com.craftstats.common.gui.BlockPosScreen;
import com.craftstats.common.gui.CraftStatsScreen;
import com.craftstats.common.gui.RandomizeScreen;
import com.craftstats.common.stats.MobStats;
import com.craftstats.common.stats.StatRegistry;
import com.craftstats.common.stats.TargetType;
import com.craftstats.common.util.ScreenOpener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(CraftStats.MOD_ID)
public class CraftStatsNeoForge {

    public CraftStatsNeoForge(IEventBus modBus, ModContainer container) {
        CraftStats.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(this::onClientSetup);
        }
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        CraftStatsClient.init();
        event.enqueueWork(CraftStatsNeoForge::registerScreenOpener);
    }

    private static void registerScreenOpener() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        ScreenOpener.setImpl(new ScreenOpener.Impl() {
            @Override
            public void openBlockEditor(Block block) {
                mc.execute(() -> mc.setScreen(new CraftStatsScreen(block)));
            }

            @Override
            public void openBlockEditorAt(String posKey, Block block, BlockPos pos) {
                mc.execute(() -> mc.setScreen(new BlockPosScreen(posKey, block, pos)));
            }

            @Override
            public void openMobEditor(LivingEntity entity) {
                mc.execute(() -> mc.setScreen(new CraftStatsScreen(entity)));
            }

            @Override
            public void openMobEditorById(String entityTypeId) {
                mc.execute(() -> mc.setScreen(
                        new CraftStatsScreen(com.craftstats.common.stats.TargetType.MOB, entityTypeId)));
            }

            @Override
            public void openItemEditor(String itemId) {
                mc.execute(() -> {
                    CraftStatsScreen screen = new CraftStatsScreen();
                    mc.setScreen(screen);
                    screen.selectItemById(itemId);
                });
            }

            @Override
            public void openPlayerEditor(Player player) {
                mc.execute(() -> mc.setScreen(
                        new com.craftstats.common.gui.PlayerStatsScreen(
                                player.getUUID(), player.getName().getString())));
            }

            @Override
            public void openRandomizeBlock(Block block) {
                String id = BuiltInRegistries.BLOCK.getKey(block).toString();
                com.craftstats.common.stats.BlockStats stats =
                        StatRegistry.getBlock(ResourceLocation.parse(id));
                if (stats == null) stats = new com.craftstats.common.stats.BlockStats();
                final com.craftstats.common.stats.BlockStats fs = stats;
                mc.execute(() -> mc.setScreen(new RandomizeScreen(TargetType.BLOCK, id, fs)));
            }

            @Override
            public void openRandomizeMob(LivingEntity entity) {
                String id = EntityType.getKey(entity.getType()).toString();
                MobStats stats = StatRegistry.getMob(ResourceLocation.parse(id));
                if (stats == null) stats = new MobStats();
                final MobStats fs = stats;
                mc.execute(() -> mc.setScreen(new RandomizeScreen(TargetType.MOB, id, fs)));
            }
        });
    }
}
