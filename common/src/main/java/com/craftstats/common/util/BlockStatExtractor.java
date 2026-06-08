package com.craftstats.common.util;

import com.craftstats.common.mixin.BlockBehaviourAccessor;
import com.craftstats.common.mixin.BlockBehaviourPropertiesAccessor;
import com.craftstats.common.stats.BlockStats;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.MagmaBlock;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.SoulSandBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.WitherRoseBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class BlockStatExtractor {

    public static BlockStats extractFrom(Block block) {
        BlockStats stats = new BlockStats();

        try {
            BlockBehaviour.Properties props =
                    ((BlockBehaviourAccessor)(Object)block).craftstats$getBlockProperties();
            BlockBehaviourPropertiesAccessor propsAcc =
                    (BlockBehaviourPropertiesAccessor)(Object)props;

            stats.hardness    = propsAcc.craftstats$getDestroyTime();
            stats.noCollision = !propsAcc.craftstats$getHasCollision();
        } catch (Exception ignored) {
            stats.hardness    = 1.5f;
            stats.noCollision = false;
        }

        stats.blastResistance = block.getExplosionResistance();
        stats.slipperiness    = block.getFriction();
        stats.lightEmission   = block.defaultBlockState().getLightEmission();

        if (block instanceof LadderBlock || block instanceof VineBlock
                || block instanceof ScaffoldingBlock) {
            stats.climbable = true;
        }

        if (block instanceof PowderSnowBlock) {
            stats.freezeOnStep = true;
        }

        if (block instanceof MagmaBlock) {
            stats.stepDamage = 1.0f;
        }

        if (block instanceof SlimeBlock) {
            stats.bounceFactor = 0.8f;
            stats.slipperiness = 0.8f;
        }

        if (block instanceof CampfireBlock || block instanceof FireBlock) {
            stats.stepDamage = 1.0f;
        }

        if (block instanceof SoulSandBlock) {
            stats.speedModifier = 0.4f;
        }

        if (block instanceof HoneyBlock) {
            stats.speedModifier = 0.4f;
            stats.bounceFactor  = 0.0f;
        }

        if (block instanceof WitherRoseBlock || block instanceof SweetBerryBushBlock) {
            stats.stepDamage = 1.0f;
        }

        return stats;
    }

    private BlockStatExtractor() {}
}
