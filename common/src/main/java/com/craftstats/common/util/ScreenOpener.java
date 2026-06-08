package com.craftstats.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;

public final class ScreenOpener {

    private static Impl impl = new NoopImpl();

    public static void setImpl(Impl i) { impl = i; }

    public static void openBlockEditor(Block block)                                { impl.openBlockEditor(block); }
    public static void openBlockEditorAt(String posKey, Block block, BlockPos pos) { impl.openBlockEditorAt(posKey, block, pos); }
    public static void openMobEditor(LivingEntity entity)                          { impl.openMobEditor(entity); }
    public static void openMobEditorById(String entityTypeId)                      { impl.openMobEditorById(entityTypeId); }
    public static void openItemEditor(String itemId)                               { impl.openItemEditor(itemId); }
    public static void openPlayerEditor(Player player)                             { impl.openPlayerEditor(player); }
    public static void openRandomizeBlock(Block block)                             { impl.openRandomizeBlock(block); }
    public static void openRandomizeMob(LivingEntity e)                            { impl.openRandomizeMob(e); }

    public interface Impl {
        void openBlockEditor(Block block);
        void openBlockEditorAt(String posKey, Block block, BlockPos pos);
        void openMobEditor(LivingEntity entity);
        void openMobEditorById(String entityTypeId);
        void openItemEditor(String itemId);
        void openPlayerEditor(Player player);
        void openRandomizeBlock(Block block);
        void openRandomizeMob(LivingEntity entity);
    }

    private static final class NoopImpl implements Impl {
        public void openBlockEditor(Block b)                               {}
        public void openBlockEditorAt(String k, Block b, BlockPos p)      {}
        public void openMobEditor(LivingEntity e)                          {}
        public void openMobEditorById(String id)                           {}
        public void openItemEditor(String id)                              {}
        public void openPlayerEditor(Player p)                             {}
        public void openRandomizeBlock(Block b)                            {}
        public void openRandomizeMob(LivingEntity e)                       {}
    }
}
