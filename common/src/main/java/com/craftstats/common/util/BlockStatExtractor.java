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
import net.minecraft.world.level.block.SoulSandBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.WitherRoseBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class BlockStatExtractor {

    /** The vanilla properties of a block, as a fully filled-in BlockStats. */
    public static BlockStats extractFrom(Block block) {
        BlockStats stats = new BlockStats();

        BlockBehaviour.Properties props = ((BlockBehaviourAccessor) block).craftstats$getBlockProperties();
        BlockBehaviourPropertiesAccessor propsAcc = (BlockBehaviourPropertiesAccessor) props;
        stats.hardness    = propsAcc.craftstats$getDestroyTime();
        stats.noCollision = !propsAcc.craftstats$getHasCollision();

        stats.blastResistance = ((BlockBehaviourAccessor) block).craftstats$getExplosionResistance();
        stats.slipperiness    = ((BlockBehaviourAccessor) block).craftstats$getFriction();
        stats.lightEmission   = block.defaultBlockState().getLightEmission();
        stats.pushReaction    = "normal";

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

        if (block instanceof CampfireBlock || block instanceof FireBlock) {
            stats.stepDamage = 1.0f;
        }

        if (block instanceof SoulSandBlock) {
            stats.speedModifier = 0.4f;
        }

        if (block instanceof HoneyBlock) {
            stats.speedModifier = 0.4f;
        }

        if (block instanceof WitherRoseBlock || block instanceof SweetBerryBushBlock) {
            stats.stepDamage = 1.0f;
        }

        return stats;
    }

    private BlockStatExtractor() {}
}
